/*
 *    Copyright 2019-2024 the original author or authors.
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

import com.backstage.app.dict.api.domain.DictFieldType;
import com.backstage.app.dict.configuration.DictsConfiguration;
import com.backstage.app.dict.configuration.backend.provider.DictSchemeBackendProvider;
import com.backstage.app.dict.configuration.properties.DictsProperties;
import com.backstage.app.dict.constant.ServiceFieldConstants;
import com.backstage.app.dict.domain.*;
import com.backstage.app.dict.exception.dict.DictAlreadyExistsException;
import com.backstage.app.dict.exception.dict.constraint.ConstraintAlreadyExistsException;
import com.backstage.app.dict.exception.dict.constraint.ConstraintNotFoundException;
import com.backstage.app.dict.exception.dict.enums.EnumAlreadyExistsException;
import com.backstage.app.dict.exception.dict.enums.EnumNotFoundException;
import com.backstage.app.dict.exception.dict.field.FieldNotFoundException;
import com.backstage.app.dict.exception.dict.index.IndexAlreadyExistsException;
import com.backstage.app.dict.exception.dict.index.IndexNotFoundException;
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

import java.time.LocalDateTime;
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

	@Cacheable(value = DictsConfiguration.CACHE_NAME_DICTS, sync = true)
	public Dict getById(String id)
	{
		return dictBackend.getDictById(id);
	}

	public List<Dict> getAll()
	{
		return dictBackend.getAllDicts();
	}

	public boolean existsById(String id)
	{
		return dictBackend.existsById(id);
	}

	@Transactional
	@CachePut(value = DictsConfiguration.CACHE_NAME_DICTS, key = "#dict.getId()")
	public Dict create(Dict dict)
	{
		if (existsById(dict.getId()))
		{
			throw new DictAlreadyExistsException(dict.getId());
		}

		if (dict.getEngine() == null)
		{
			dict.setEngine(new DictEngine(DictsProperties.DEFAULT_ENGINE));
		}

		dictValidationService.validateDictScheme(dict, this);

		mapDefaultFieldValues(dict);

		var created = buildScheme(dict, new Dict());

		schemeBackend(dict).createDictScheme(created);

		var savedDict = dictBackend.saveDict(created);

		dictLockService.addLock(savedDict.getId());

		return savedDict;
	}

	@Transactional
	//TODO: рассмотреть необходимость обновления схемы для DictConstraint/DictIndex
	// сейчас обновляется схема только для DictField и DictEnum, последний только в монго.
	@LockDictSchemaModifyOperation("#dictId")
	@CachePut(value = DictsConfiguration.CACHE_NAME_DICTS, key = "#dictId")
	public Dict update(String dictId, Dict dict)
	{
		var actualDict = getById(dictId);

		//TODO: разработать обновление engine через api
		if (dict.getEngine() == null)
		{
			dict.setEngine(actualDict.getEngine());
		}

		dictValidationService.validateDictScheme(dict, this);

		mapDefaultFieldValues(dict);

		var actualDictEngine = actualDict.getEngine();
		var targetDictEngine = dict.getEngine();

		var updated = buildScheme(dict, actualDict);

		if (actualDictEngine != null && !actualDictEngine.getName().equals(targetDictEngine.getName()))
		{
			dictStorageMigrationService.migrate(updated, actualDictEngine, targetDictEngine);
		}

		schemeBackend(dict).updateDictScheme(updated);

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

		return dictBackend.updateDict(updated);
	}

	@Transactional
	//	TODO: История изменений схемы, даты создания/обновления схемы?
	@LockDictSchemaModifyOperation("#dictId")
	@CacheEvict(value = DictsConfiguration.CACHE_NAME_DICTS, key = "#dictId")
	public void delete(String dictId, boolean deleted)
	{
		dictBackend.softDelete(dictId, deleted ? LocalDateTime.now() : null);
	}

	@Transactional
	@LockDictSchemaModifyOperation("#dictId")
	@CacheEvict(value = DictsConfiguration.CACHE_NAME_DICTS, key = "#dictId")
	public DictField renameField(String dictId, String fieldId, String newFieldId, String newFieldName)
	{
		var dict = getById(dictId);

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

		return renamed;
	}

	@Transactional
	@LockDictSchemaModifyOperation("#dictId")
	@CacheEvict(value = DictsConfiguration.CACHE_NAME_DICTS, key = "#dictId")
	public DictConstraint createConstraint(String dictId, DictConstraint constraint)
	{
		var dict = getById(dictId);

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

		return created;
	}

	@Transactional
	@LockDictSchemaModifyOperation("#dictId")
	@CacheEvict(value = DictsConfiguration.CACHE_NAME_DICTS, key = "#dictId")
	public void deleteConstraint(String dictId, String constraintId)
	{
		var dict = getById(dictId);

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
		var dict = getById(dictId);

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

		return created;
	}

	@Transactional
	@LockDictSchemaModifyOperation("#dictId")
	@CacheEvict(value = DictsConfiguration.CACHE_NAME_DICTS, key = "#dictId")
	public void deleteIndex(String dictId, String indexId)
	{
		var dict = getById(dictId);

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
		var dict = getById(dictId);

		var exists = dict.getEnums()
				.stream()
				.anyMatch(it -> it.getId().equals(dictEnum.getId()));

		if (exists)
		{
			throw new EnumAlreadyExistsException(dictEnum.getId());
		}

//		TODO: Валидация - ни один из адапатеров самостоятельно не добавил enum
		dict.getEnums().add(dictEnum);

		return dictBackend.createEnum(dict, dictEnum);
	}

	@Transactional
	@LockDictSchemaModifyOperation("#dictId")
	@CacheEvict(value = DictsConfiguration.CACHE_NAME_DICTS, key = "#dictId")
	public DictEnum updateEnum(String dictId, DictEnum dictEnum)
	{
		var dict = getById(dictId);

		return dictBackend.updateEnum(dict, dictEnum);
	}

	@Transactional
	@LockDictSchemaModifyOperation("#dictId")
	@CacheEvict(value = DictsConfiguration.CACHE_NAME_DICTS, key = "#dictId")
	public void deleteEnum(String dictId, String enumId)
	{
		var dict = getById(dictId);

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

	// TODO: кэш
	public static List<DictField> getDataFieldsByDict(Dict dict)
	{
		return dict.getFields()
				.stream()
				.filter(it -> !ServiceFieldConstants.getServiceSchemeFields().contains(it.getId()))
//				FIXME: На данном этапе коллекция должна быть изменяемой
				.collect(Collectors.toList());
	}

	// TODO: кэш
	public static Map<String, DictField> getReferenceFieldMap(Dict dict)
	{
		return dict.getFields()
				.stream()
				.filter(field -> DictFieldType.DICT.equals(field.getType()))
				.collect(Collectors.toMap(DictField::getId, Function.identity()));
	}

	private Dict buildScheme(Dict source, Dict target)
	{
		addServiceFields(source.getFields());

		target.setId(source.getId());
		target.setName(source.getName());
		target.setFields(source.getFields());
		target.setViewPermission(source.getViewPermission());
		target.setEditPermission(source.getEditPermission());
		target.setDeleted(source.getDeleted());
		target.setIndexes(source.getIndexes());
		target.setConstraints(source.getConstraints());
		target.setEngine(source.getEngine());

		return target;
	}

	private void addServiceFields(List<DictField> dictFields)
	{
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
						.id(DELETED)
						.name("Дата удаления")
						.type(DictFieldType.TIMESTAMP)
						.required(false)
						.multivalued(false)
						.build());

		dictFields.add(
				DictField.builder()
						.id(DELETION_REASON)
						.name("Причина удаления")
						.type(DictFieldType.STRING)
						.required(false)
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
