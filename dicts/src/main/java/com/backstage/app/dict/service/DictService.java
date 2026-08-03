/*
 *    Copyright 2019-2026 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.backstage.app.dict.service;

import com.backstage.app.cache.utils.proxy.ReadOnlyObjectProxyFactory;
import com.backstage.app.dict.api.domain.DictFieldType;
import com.backstage.app.dict.configuration.DictsConfiguration;
import com.backstage.app.dict.configuration.backend.provider.DictSchemeBackendProvider;
import com.backstage.app.dict.configuration.properties.DictsProperties;
import com.backstage.app.dict.constant.ServiceFieldConstants;
import com.backstage.app.dict.domain.*;
import com.backstage.app.dict.domain.scheme.DictNativeScheme;
import com.backstage.app.dict.exception.dict.DictAlreadyExistsException;
import com.backstage.app.dict.exception.dict.DictException;
import com.backstage.app.dict.exception.dict.constraint.ConstraintAlreadyExistsException;
import com.backstage.app.dict.exception.dict.constraint.ConstraintNotFoundException;
import com.backstage.app.dict.exception.dict.enums.EnumAlreadyExistsException;
import com.backstage.app.dict.exception.dict.enums.EnumNotFoundException;
import com.backstage.app.dict.exception.dict.field.FieldNotFoundException;
import com.backstage.app.dict.exception.dict.index.IndexAlreadyExistsException;
import com.backstage.app.dict.exception.dict.index.IndexNotFoundException;
import com.backstage.app.dict.service.advice.DictServiceAdvice;
import com.backstage.app.dict.service.backend.DictBackend;
import com.backstage.app.dict.service.backend.DictSchemeBackend;
import com.backstage.app.dict.service.lock.DictLockService;
import com.backstage.app.dict.service.lock.LockDictSchemaModifyOperation;
import com.backstage.app.dict.service.mapping.DictItemMappingService;
import com.backstage.app.dict.service.migration.DictStorageMigrationService;
import com.backstage.app.dict.service.validation.DictValidationService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.backstage.app.dict.constant.ServiceFieldConstants.*;

@Service
@RequiredArgsConstructor
public class DictService
{
	private final DictBackend dictBackend;

	private final DictSchemeBackendProvider schemeBackendProvider;

	private final DictLockService dictLockService;

	private final DictStorageMigrationService dictStorageMigrationService;

	private final DictValidationService dictValidationService;

	private final DictItemMappingService dictItemMappingService;

	private final List<DictServiceAdvice> serviceAdviceList;

	@Cacheable(value = DictsConfiguration.CACHE_NAME_DICTS, sync = true)
	public Dict getById(String id)
	{
		serviceAdviceList.forEach(it -> it.handleGetById(id));

		return getByIdInternal(id);
	}

	private Dict getByIdInternal(String id)
	{
		return ReadOnlyObjectProxyFactory.createProxy(dictBackend.getDictById(id));
	}

	// TODO: кэш
	// TODO: добавить internal методы по аналогии с getById, чтобы не вызывать advice'ы на каждый внутренний вызов.
	public List<Dict> getAll()
	{
		serviceAdviceList.forEach(DictServiceAdvice::handleGetAll);

		return dictBackend.getAllDicts();
	}

	public boolean existsById(String id)
	{
		serviceAdviceList.forEach(it -> it.handleExistsById(id));

		return dictBackend.existsById(id);
	}

	@Transactional
	@CachePut(value = DictsConfiguration.CACHE_NAME_DICTS, key = "#newDict.getId()")
	public Dict create(Dict newDict)
	{
		if (existsById(newDict.getId()))
		{
			throw new DictAlreadyExistsException(newDict.getId());
		}

		var dict = incrementDictVersion(newDict);

		serviceAdviceList.forEach(it -> it.handleBeforeCreate(dict));

		if (dict.getEngine() == null)
		{
			dict.setEngine(new DictEngine(DictsProperties.DEFAULT_ENGINE));
		}

		dictValidationService.validateDictScheme(dict, this);

		schemeBackend(dict).createDictScheme(dict);

		var savedDict = dictBackend.saveDict(dict);

		dictLockService.addLock(savedDict.getId());

		serviceAdviceList.forEach(it -> it.handleAfterCreate(dict));

		return ReadOnlyObjectProxyFactory.createProxy(savedDict);
	}

	@Transactional
	//TODO: рассмотреть необходимость обновления схемы для DictConstraint/DictIndex
	// сейчас обновляется схема только для DictField и DictEnum, последний только в монго.
	@LockDictSchemaModifyOperation("#dictId")
	@CachePut(value = DictsConfiguration.CACHE_NAME_DICTS, key = "#dictId")
	@CacheEvict(
			value = {
					DictsConfiguration.CACHE_NAME_DICT_DATA_FIELDS,
					DictsConfiguration.CACHE_NAME_DICT_SCHEMES
			},
			key = "#dictId"
	)
	public Dict update(String dictId, Dict dict)
	{
		var actualDict = getByIdInternal(dictId);

		serviceAdviceList.forEach(it -> it.handleBeforeUpdate(actualDict, dict));

		//TODO: разработать обновление engine через api
		if (dict.getEngine() == null)
		{
			dict.setEngine(actualDict.getEngine());
		}

		dictValidationService.validateDictScheme(dict, this);

		var actualDictEngine = actualDict.getEngine();
		var targetDictEngine = dict.getEngine();

		var updated = dict.copy();

		updated.setId(actualDict.getId());
		updated.setVersion(actualDict.getVersion());

		updated = incrementDictVersion(updated);

		if (actualDictEngine != null && !actualDictEngine.getName().equals(targetDictEngine.getName()))
		{
			dictStorageMigrationService.migrate(updated, actualDictEngine, targetDictEngine);
		}

		var schemeBackend = schemeBackend(updated);

		schemeBackend.updateDictScheme(updated);

		var updatedFieldIds = updated.getFields()
				.stream()
				.map(DictField::getId)
				.collect(Collectors.toSet());

		var actualIndexes = updated.getIndexes()
				.stream()
				.filter(it -> updatedFieldIds.containsAll(it.getFields()))
				.toList();

		var actualConstraints = updated.getConstraints()
				.stream()
				.filter(it -> updatedFieldIds.containsAll(it.getFields()))
				.toList();

		updated.setIndexes(actualIndexes);
		updated.setConstraints(actualConstraints);

		var updateDict = dictBackend.updateDict(updated);

		var actualDictFieldIds = actualDict.getFieldIds();

		updated.getFields()
				.stream()
				.filter(it -> it.getType() == DictFieldType.SERIAL)
				.filter(it -> !actualDictFieldIds.contains(it.getId()))
				.forEach(serialField -> {
					schemeBackend.restartSerialField(dictId, serialField.getId(), 1L);
				});

		serviceAdviceList.forEach(it -> it.handleAfterUpdate(updateDict));

		return updateDict;
	}

	private Dict incrementDictVersion(Dict dict)
	{
		dict = dict.copy();

		addServiceFields(dict.getFields());

		// TODO: убрать в валидацию
		mapDefaultFieldValues(dict);

		var version = dict.getVersion() == null
				? 1L
				: dict.getVersion() + 1;

		dict.setVersion(version);

		return dict;
	}

	@Transactional
	//	TODO: История изменений схемы, даты создания/обновления схемы?
	@LockDictSchemaModifyOperation("#dictId")
	@CacheEvict(
			value = {
					DictsConfiguration.CACHE_NAME_DICTS,
					DictsConfiguration.CACHE_NAME_DICT_DATA_FIELDS,
					DictsConfiguration.CACHE_NAME_DICT_SCHEMES},
			key = "#dictId"
	)
	public void delete(String dictId)
	{
		var dict = getByIdInternal(dictId);

		serviceAdviceList.forEach(it -> it.handleDelete(dict));

		dictValidationService.validateDrop(dictId);

		schemeBackend(dict).deleteDictSchemeById(dictId);
		dictBackend.deleteById(dictId);
	}

	@Transactional
	@LockDictSchemaModifyOperation("#dictId")
	@CacheEvict(
			value = {
					DictsConfiguration.CACHE_NAME_DICTS,
					DictsConfiguration.CACHE_NAME_DICT_DATA_FIELDS,
					DictsConfiguration.CACHE_NAME_DICT_SCHEMES
			},
			key = "#dictId"
	)
	public DictField renameField(String dictId, String fieldId, String newFieldId, String newFieldName)
	{
		var dict = incrementDictVersion(getByIdInternal(dictId));

		serviceAdviceList.forEach(it -> it.handleRenameField(dict, fieldId, newFieldId, newFieldName));

		var field = dict.getFields()
				.stream()
				.filter(it -> it.getId().equals(fieldId))
				.peek(it -> {
					it.setId(newFieldId);
					it.setName(newFieldName == null ? it.getName() : newFieldName);
				})
				.findFirst()
				.orElseThrow(() -> new FieldNotFoundException(dictId, fieldId));

		var renamed = schemeBackend(dict).renameDictField(dict, fieldId, field);

		var actualIndexes = dict.getIndexes()
				.stream()
				.peek(it -> it.getFields().replaceAll(f -> StringUtils.equals(f, fieldId) ? renamed.getId() : f))
				.toList();

		var actualConstraints = dict.getConstraints()
				.stream()
				.peek(it -> it.getFields().replaceAll(f -> StringUtils.equals(f, fieldId) ? renamed.getId() : f))
				.toList();

		dict.setIndexes(actualIndexes);
		dict.setConstraints(actualConstraints);

		dictBackend.updateDict(dict);

		return ReadOnlyObjectProxyFactory.createProxy(renamed);
	}

	@Transactional
	@LockDictSchemaModifyOperation("#dictId")
	@CacheEvict(value = DictsConfiguration.CACHE_NAME_DICTS, key = "#dictId")
	public DictConstraint createConstraint(String dictId, DictConstraint constraint)
	{
		var dict = incrementDictVersion(getByIdInternal(dictId));

		serviceAdviceList.forEach(it -> it.handleCreateConstraint(dict, constraint));

//		TODO: Валидация - в validationService
		var dictConstraintAlreadyExistsCondition = dict.getConstraints()
				.stream()
				.anyMatch(it -> it.getId().equals(constraint.getId()));

		if (dictConstraintAlreadyExistsCondition)
		{
			throw new ConstraintAlreadyExistsException(dictId, constraint.getId());
		}

		var dictIndexAlreadyExistsCondition = dict.getIndexes()
				.stream()
				.anyMatch(it -> it.getId().equals(constraint.getId()));

		if (dictIndexAlreadyExistsCondition)
		{
			throw new IndexAlreadyExistsException(dictId, constraint.getId());
		}

		var created = schemeBackend(dict).createConstraint(dict, constraint);

//		TODO: Валидация - ни один из адапатеров самостоятельно не добавил constraint
		dict.getConstraints().add(created);

		dictBackend.updateDict(dict);

		return ReadOnlyObjectProxyFactory.createProxy(created);
	}

	@Transactional
	@LockDictSchemaModifyOperation("#dictId")
	@CacheEvict(value = DictsConfiguration.CACHE_NAME_DICTS, key = "#dictId")
	public void deleteConstraint(String dictId, String constraintId)
	{
		var dict = incrementDictVersion(getByIdInternal(dictId));

		serviceAdviceList.forEach(it -> it.handleDeleteConstraint(dict, constraintId));

		var constraintNotFoundCondition = dict.getConstraints()
				.stream()
				.noneMatch(it -> it.getId().equals(constraintId));

		if (constraintNotFoundCondition)
		{
			throw new ConstraintNotFoundException(dictId, constraintId);
		}

		schemeBackend(dict).deleteConstraint(dict, constraintId);

//		TODO: Валидация - ни один из адапатеров самостоятельно не удалил constraint
		var actualConstraints = dict.getConstraints()
				.stream()
				.filter(it -> !StringUtils.equals(it.getId(), constraintId))
				.collect(Collectors.toList());

		dict.setConstraints(actualConstraints);

		dictBackend.updateDict(dict);
	}

	@Transactional
	@LockDictSchemaModifyOperation("#dictId")
	@CacheEvict(value = DictsConfiguration.CACHE_NAME_DICTS, key = "#dictId")
	public DictIndex createIndex(String dictId, DictIndex index)
	{
		var dict = incrementDictVersion(getByIdInternal(dictId));

		serviceAdviceList.forEach(it -> it.handleCreateIndex(dict, index));

//		TODO: Валидация - в validationService
		var indexAlreadyExistsCondition = dict.getIndexes()
				.stream()
				.anyMatch(it -> it.getId().equals(index.getId()));

		if (indexAlreadyExistsCondition)
		{
			throw new IndexAlreadyExistsException(dictId, index.getId());
		}

		var constraintAlreadyExistsCondition = dict.getConstraints()
				.stream()
				.anyMatch(it -> it.getId().equals(index.getId()));

		if (constraintAlreadyExistsCondition)
		{
			throw new ConstraintAlreadyExistsException(dictId, index.getId());
		}

		var created = schemeBackend(dict).createIndex(dict, index);

//		TODO: Валидация - ни один из адапатеров самостоятельно не добавил index
		dict.getIndexes().add(created);

		dictBackend.updateDict(dict);

		return ReadOnlyObjectProxyFactory.createProxy(created);
	}

	@Transactional
	@LockDictSchemaModifyOperation("#dictId")
	@CacheEvict(value = DictsConfiguration.CACHE_NAME_DICTS, key = "#dictId")
	public void deleteIndex(String dictId, String indexId)
	{
		var dict = incrementDictVersion(getByIdInternal(dictId));

		serviceAdviceList.forEach(it -> it.handleDeleteIndex(dict, indexId));

		var indexNotFoundCondition = dict.getIndexes()
				.stream()
				.noneMatch(it -> it.getId().equals(indexId));

		if (indexNotFoundCondition)
		{
			throw new IndexNotFoundException(dictId, indexId);
		}

		schemeBackend(dict).deleteIndex(dict, indexId);

		//		TODO: Валидация - ни один из адапатеров самостоятельно не удалил index
		var actualIndexes = dict.getIndexes()
				.stream()
				.filter(it -> !StringUtils.equals(it.getId(), indexId))
				.collect(Collectors.toList());

		dict.setIndexes(actualIndexes);

		dictBackend.updateDict(dict);
	}

	@Transactional
	@LockDictSchemaModifyOperation("#dictId")
	@CacheEvict(value = DictsConfiguration.CACHE_NAME_DICTS, key = "#dictId")
	public DictEnum createEnum(String dictId, DictEnum dictEnum)
	{
		var dict = incrementDictVersion(getByIdInternal(dictId));

		serviceAdviceList.forEach(it -> it.handleCreateEnum(dict, dictEnum));

		var exists = dict.getEnums()
				.stream()
				.anyMatch(it -> it.getId().equals(dictEnum.getId()));

		if (exists)
		{
			throw new EnumAlreadyExistsException(dictEnum.getId());
		}

//		TODO: Валидация - ни один из адапатеров самостоятельно не добавил enum
		dict.getEnums().add(dictEnum);

		return ReadOnlyObjectProxyFactory.createProxy(dictBackend.createEnum(dict, dictEnum));
	}

	@Transactional
	@LockDictSchemaModifyOperation("#dictId")
	@CacheEvict(value = DictsConfiguration.CACHE_NAME_DICTS, key = "#dictId")
	public DictEnum updateEnum(String dictId, DictEnum dictEnum)
	{
		var dict = incrementDictVersion(getByIdInternal(dictId));

		serviceAdviceList.forEach(it -> it.handleUpdateEnum(dict, dictEnum));

		return ReadOnlyObjectProxyFactory.createProxy(dictBackend.updateEnum(dict, dictEnum));
	}

	@Transactional
	@LockDictSchemaModifyOperation("#dictId")
	@CacheEvict(value = {DictsConfiguration.CACHE_NAME_DICTS}, key = "#dictId")
	public void deleteEnum(String dictId, String enumId)
	{
		var dict = incrementDictVersion(getByIdInternal(dictId));

		serviceAdviceList.forEach(it -> it.handleDeleteEnum(dict, enumId));

		var exists = dict.getEnums()
				.stream()
				.anyMatch(it -> it.getId().equals(enumId));

		if (!exists)
		{
			throw new EnumNotFoundException(enumId);
		}

//		TODO: Валидация - ни один из адапатеров самостоятельно не удалил enum
		var actualEnums = dict.getEnums()
				.stream()
				.filter(it -> !StringUtils.equals(it.getId(), enumId))
				.toList();

		dict.setEnums(actualEnums);

		dictBackend.deleteEnum(dict, enumId);
	}

	@Cacheable(value = DictsConfiguration.CACHE_NAME_DICT_DATA_FIELDS, key = "#dict.id")
	public List<DictField> getDataFieldsByDict(Dict dict)
	{
		return dict.getFields()
				.stream()
				.filter(it -> !ServiceFieldConstants.getServiceSchemeFields().contains(it.getId()))
				.toList();
	}

	@Cacheable(value = DictsConfiguration.CACHE_NAME_DICT_SCHEMES, sync = true)
	public DictNativeScheme getNativeScheme(String dictId)
	{
		var dict = getByIdInternal(dictId);

		return schemeBackend(dict).getNativeScheme(dict);
	}

	public void restartSerialField(String dictId, String fieldId, Long startWithValue)
	{
		var dict = getByIdInternal(dictId);
		var field = dict.getFields()
				.stream()
				.filter(it -> it.getId().equals(fieldId))
				.findFirst()
				.orElseThrow(() -> new FieldNotFoundException(dictId, fieldId));

		if (field.getType() != DictFieldType.SERIAL)
		{
			throw new DictException("Поле %s справочника %s не является полем с автоматическим увеличением значения.".formatted(dictId, fieldId));
		}

		if (startWithValue < 1)
		{
			throw new DictException("Начальное значение поля должно быть более или равно 1.");
		}

		schemeBackend(dict).restartSerialField(dictId, fieldId, startWithValue);
	}

	// TODO: кэш
	public static Map<String, DictField> getReferenceFieldMap(Dict dict)
	{
		return dict.getFields()
				.stream()
				.filter(field -> DictFieldType.DICT.equals(field.getType()))
				.collect(Collectors.toMap(DictField::getId, Function.identity()));
	}

	private void addServiceFields(List<DictField> dictFields)
	{
		if (dictFields.stream().anyMatch(f -> ServiceFieldConstants.ID.equals(f.getId())))
		{
			return;
		}

		dictFields.add(0, DictField.builder()
				.id(ID)
				.name("Идентификатор")
				.type(DictFieldType.STRING)
				.required(false)
				.multivalued(false)
				.build());

		dictFields.add(
				DictField.builder()
						.id(CREATED)
						.name("Дата создания")
						.type(DictFieldType.TIMESTAMP)
						.required(true)
						.multivalued(false)
						.build());

		dictFields.add(
				DictField.builder()
						.id(UPDATED)
						.name("Дата обновления")
						.type(DictFieldType.TIMESTAMP)
						.required(true)
						.multivalued(false)
						.build());

		dictFields.add(
				DictField.builder()
						.id(HISTORY)
						.name("История изменений")
						.type(DictFieldType.JSON)
						.required(true)
						.multivalued(true)
						.build());

		dictFields.add(
				DictField.builder()
						.id(VERSION)
						.name("Версия")
						.type(DictFieldType.INTEGER)
						.required(true)
						.multivalued(false)
						.build());
	}

	private DictSchemeBackend schemeBackend(Dict dict)
	{
		return schemeBackendProvider.getBackendByEngineName(dict.getEngine().getName());
	}

	private void mapDefaultFieldValues(Dict dict)
	{
		dict.getFields()
				.forEach(field -> field.setDefaultValue(mapDefaultFieldValue(field)));
	}

	private Object mapDefaultFieldValue(DictField field)
	{
		return field.getDefaultValue() == null
				? null
				: dictItemMappingService.mapField(field, field.getDefaultValue());
	}
}
