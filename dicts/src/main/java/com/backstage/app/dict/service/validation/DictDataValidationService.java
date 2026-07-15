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

package com.backstage.app.dict.service.validation;

import com.backstage.app.dict.api.domain.DictFieldType;
import com.backstage.app.dict.constant.ServiceFieldConstants;
import com.backstage.app.dict.domain.Dict;
import com.backstage.app.dict.domain.DictConstraint;
import com.backstage.app.dict.domain.DictField;
import com.backstage.app.dict.domain.DictFieldName;
import com.backstage.app.dict.domain.DictItem;
import com.backstage.app.dict.exception.dict.DictConcurrentUpdateException;
import com.backstage.app.dict.exception.dict.DictException;
import com.backstage.app.dict.exception.dict.UnavailableDictRefException;
import com.backstage.app.dict.exception.dict.enums.EnumNotFoundException;
import com.backstage.app.dict.exception.dict.field.FieldNotFoundException;
import com.backstage.app.dict.exception.dict.field.FieldValidationException;
import com.backstage.app.dict.exception.dict.field.ForbiddenFieldNameException;
import com.backstage.app.dict.service.DictDataService;
import com.backstage.app.dict.service.DictService;
import com.backstage.app.dict.service.backend.postgres.PostgresEngine;
import com.backstage.app.dict.service.mapping.DictFieldNameMappingService;
import com.backstage.app.exception.ObjectNotFoundException;
import com.backstage.app.model.other.date.DateConstants;
import com.backstage.app.utils.DateUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.geojson.GeoJsonObject;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class DictDataValidationService
{
	private final ObjectMapper objectMapper;

	private final DictService dictService;
	private final DictDataService dictDataService;

	private final DictFieldNameMappingService fieldNameMappingService;

	public DictDataValidationService(@Lazy DictService dictService, @Lazy DictDataService dictDataService, ObjectMapper objectMapper, DictFieldNameMappingService fieldNameMappingService)
	{
		this.dictService = dictService;
		this.dictDataService = dictDataService;
		this.objectMapper = objectMapper;
		this.fieldNameMappingService = fieldNameMappingService;
	}

	public void validateDeleteAll(Dict dict)
	{
		dictService.getAll()
				.stream()
				.filter(it -> !it.getId().equals(dict.getId()))
				.forEach(relatedDict -> validateRelatedDictReferences(dict.getId(), relatedDict));
	}

	public void validateDelete(Dict dict, String itemId)
	{
		var relatedDictIds = dictService.getAll()
				.stream()
				.filter(relatedDict -> relatedItemExists(dict, relatedDict, itemId))
				.map(Dict::getId)
				.toList();

		if (!relatedDictIds.isEmpty())
		{
			throw new DictException(
					"Невозможно удалить записи из справочника '%s': в других справочниках присутствуют ссылки на запись."
							.formatted(dict.getId())
			);
		}
	}

	public void validateSelectFields(Dict dict, List<DictFieldName> selectFields)
	{
		validateRefDict(dict, selectFields);

		var withoutRefFields = selectFields.stream()
				.filter(it -> it.getDictId() == null)
				.map(DictFieldName::getFieldId)
				.toList();

		validateFieldsBySingleScheme(dict, withoutRefFields);

		selectFields.stream()
				.filter(it -> it.getDictId() != null)
				.collect(Collectors.groupingBy(
						DictFieldName::getDictId, Collectors.mapping(DictFieldName::getFieldId, Collectors.toList()))
				)
				.forEach(this::validateFieldsBySingleScheme);
	}

	public void validatePageable(Dict dict, Pageable pageable)
	{
		if (pageable != null)
		{
			var sortedFields = pageable.getSort()
					.stream()
					.map(Sort.Order::getProperty)
					.map(this::dictFieldName)
					.toList();

			var withoutRefFields = sortedFields.stream()
					.filter(it -> it.getDictId() == null)
					.map(DictFieldName::getFieldId)
					.toList();

			validateFieldsBySingleScheme(dict, withoutRefFields);

			validateRefDict(dict, sortedFields);
		}
	}

	private DictFieldName dictFieldName(String field)
	{
		return fieldNameMappingService.mapDictFieldName(field);
	}

	//TODO: Актуализировать, отказавшись от проброса userId
	public void validateDictDataItem(String dictId, DictItem dictItem, String userId)
	{
		var dict = dictService.getById(dictId);
		var dataItemMap = dictItem.getData();

		var availableFields = dict.getFields();
		var availableFieldIds = getAvailableFieldIds(availableFields);

		dataItemMap.forEach((field, value) -> checkForbiddenField(availableFieldIds, field));

		validateRequiredFields(dictId, dataItemMap, availableFields, userId);
		validateUniqueConstraints(dict, dictItem, userId);
	}

	public void validateOptimisticLock(String dictId, String itemId, long version, String userId)
	{
		var current = dictDataService.getById(dictId, itemId, userId);

		if (version != current.getVersion())
		{
			throw new DictConcurrentUpdateException(version, current.getVersion());
		}
	}

	private void validateRequiredFields(String dictId, Map<String, Object> dictData, List<DictField> availableFields, String userId)
	{
		availableFields.stream()
				.filter(field -> !ServiceFieldConstants.getServiceInsertableFields().contains(field.getId()))
				.peek(dictField -> checkCast(dictId, dictField, dictData.get(dictField.getId()), userId))
				.filter(DictField::isRequired)
				.filter(field -> field.getType() != DictFieldType.SERIAL)
				.filter(field -> field.getDefaultValue() == null)
				.filter(dictField -> !dictData.containsKey(dictField.getId()) || dictData.get(dictField.getId()) == null)
				.findAny()
				.ifPresent(field -> {
					throw new FieldValidationException("Отсутствует обязательное поле: %s.".formatted(field.getId()));
				});
	}

	private void validateUniqueConstraints(Dict dict, DictItem dictItem, String userId)
	{
		var data = dictItem.getData();
		var itemId = dictItem.getId();
		var dictFieldMap = dict.getFields()
				.stream()
				.collect(Collectors.toMap(DictField::getId, Function.identity()));

		dict.getConstraints()
				.stream()
				.filter(constraint -> isApplicable(dict, constraint, data))
				.forEach(constraint -> {
					var queryRows = constraint.getFields()
							.stream()
							.map(fieldId -> computeQueryRow(dictFieldMap.get(fieldId), data.get(fieldId)))
							.collect(Collectors.toCollection(LinkedHashSet::new));

					if (itemId != null)
					{
						queryRows.add("id != '%s'".formatted(itemId));
					}

					if (dictDataService.existsByFilter(dict.getId(), String.join(" and ", queryRows), userId))
					{
						throw new FieldValidationException("Нарушено ограничение уникального ключа: %s.".formatted(constraint.getId()));
					}
				});
	}

	/**
	 * В Postgres engine NULL не участвует в unique-сравнении, несколько NULL допустимы.
	 * В Mongo NULL — полноценное значение уникального индекса.
	 */
	private boolean isApplicable(Dict dict, DictConstraint constraint, Map<String, Object> data)
	{
		if (!PostgresEngine.POSTGRES.equals(dict.getEngine().getName()))
		{
			return true;
		}

		return constraint.getFields()
				.stream()
				.allMatch(fieldId -> data.get(fieldId) != null);
	}

	private String computeQueryRow(DictField field, Object value)
	{
		if (value == null)
		{
			return "%s is null".formatted(field.getId());
		}

		return "%s = %s".formatted(field.getId(), computeQueryValue(field, value));
	}

	private String computeQueryValue(DictField field, Object value)
	{
		return switch (field.getType())
		{
			case STRING, ATTACHMENT, DICT, ENUM -> "'%s'".formatted(value);
			case INTEGER, SERIAL, DECIMAL, BOOLEAN -> "%s".formatted(value);
			case DATE -> "'%s'::date".formatted(formatDateValue(value));
			case TIMESTAMP -> "'%s'::timestamp".formatted(formatTimestampValue(value));

			default -> throw new FieldValidationException("Поле '%s' с типом '%s' не поддерживается в уникальном ключе.".formatted(field.getId(), field.getType()));
		};
	}

	private String formatDateValue(Object value)
	{
		if (value instanceof String stringValue)
		{
			return stringValue;
		}

		if (value instanceof LocalDate localDate)
		{
			return DateConstants.ISO_DATE_FORMATTER.format(localDate);
		}

		if (value instanceof Date date)
		{
			return DateConstants.ISO_DATE_FORMATTER.format(DateUtils.toLocalDate(date));
		}

		throw new FieldValidationException("Некорректный формат date значения для уникального ключа: %s.".formatted(value));
	}

	private String formatTimestampValue(Object value)
	{
		if (value instanceof String stringValue)
		{
			return stringValue;
		}

		if (value instanceof LocalDateTime localDateTime)
		{
			return DateConstants.ISO_DATE_TIME_SECONDS_FORMATTER.format(localDateTime);
		}

		if (value instanceof Date date)
		{
			return DateConstants.ISO_DATE_TIME_SECONDS_FORMATTER.format(DateUtils.toLocalDateTime(date));
		}

		throw new FieldValidationException("Некорректный формат timestamp значения для уникального ключа: %s.".formatted(value));
	}

	public void checkCast(String dictId, DictField dictField, Object value, String userId)
	{
		if (value == null)
		{
			return;
		}
		if (value instanceof List<?> list)
		{
			if (dictField.isMultivalued())
			{
				list.forEach(it -> checkSingleElementCast(dictId, dictField, it, userId));
			}
			else
			{
				throw new FieldValidationException("Не может быть массивом: %s.".formatted(dictField.getId()));
			}
		}
		else
		{
			checkSingleElementCast(dictId, dictField, value, userId);
		}
	}

	//TODO: провести декомпозицию метода
	private void checkSingleElementCast(String dictId, DictField dictField, Object value, String userId)
	{
		try
		{
			switch (dictField.getType())
			{
				case SERIAL, INTEGER -> {
					if (checkNumberValue((Long) value, (Integer) dictField.getMinSize(), (Integer) dictField.getMaxSize()))
					{
						throw new FieldValidationException("Превышена допустимые ограничения числа: %s.".formatted(dictField.getId()));
					}
				}
				case DECIMAL -> {
					if (checkNumberValue(((BigDecimal) value).doubleValue(), dictField.getMinSize(), dictField.getMaxSize()))
					{
						throw new FieldValidationException("Превышена допустимые ограничения вещественного числа: %s.".formatted(dictField.getId()));
					}
				}
				case BOOLEAN -> Assert.isInstanceOf(Boolean.class, value, "Недопустимый тип '%s' для поля '%s'.".formatted(value.getClass().getTypeName(), dictField.getId()));
				case STRING -> {
					Assert.isInstanceOf(String.class, value, "Недопустимый тип '%s' для поля '%s'.".formatted(value.getClass().getTypeName(), dictField.getId()));

					if (checkStringLength(((String) value).length(), (Integer) dictField.getMinSize(), (Integer) dictField.getMaxSize()))
					{
						throw new FieldValidationException("Превышена допустимая длина строки: %s.".formatted(dictField.getId()));
					}
				}
				case DICT -> {
					try
					{
						dictDataService.getById(dictField.getDictRef().getDictId(), (String) value, userId);
					}
					catch (ObjectNotFoundException e)
					{
						throw new FieldValidationException("Значение отсутствует в связанном справочнике: %s.".formatted(dictField.getId()));
					}
				}
				case DATE -> {
					if (value instanceof Date || value instanceof LocalDate)
					{
						break;
					}

					LocalDate.parse((String) value, DateConstants.ISO_DATE_FORMATTER);
				}
				case TIMESTAMP -> {
					if (value instanceof Date || value instanceof LocalDateTime)
					{
						break;
					}

					LocalDateTime.parse((String) value, DateConstants.ISO_OFFSET_DATE_TIME_MS_FORMATTER);
				}
				case JSON -> {
					if (value instanceof String s)
					{
						try
						{
							objectMapper.readValue(s, Map.class);
						}
						catch (JsonProcessingException ex)
						{
							throw new FieldValidationException("Некорректный формат json поля: %s.".formatted(dictField.getId()));
						}
					}
					else
					{
						var o = (Map<?, ?>) value;
					}
				}
				case ENUM -> {
					var contains = dictService.getById(dictId)
							.getEnums()
							.stream()
							.filter(it -> it.getId().equals(dictField.getEnumId()))
							.findAny()
							.orElseThrow(() -> new EnumNotFoundException(dictField.getEnumId()))
							.getValues()
							.contains((String) value);

					if (!contains)
					{
						throw new FieldValidationException("Неизвестное значение enum: %s.".formatted(dictField.getId()));
					}
				}
				case GEO_JSON -> {
					if (value instanceof GeoJsonObject)
					{
						return;
					}

					//TODO: реализовать персистентное хранение значений GEO_JSON как GeoJson обьектов (postgis)
					if (value instanceof String s)
					{
						objectMapper.readValue(s, GeoJsonObject.class);

						return;
					}

					throw new FieldValidationException("Недопустимый тип '%s' значения для поля: %s".formatted(value.getClass().getTypeName(), dictField.getId()));
				}
				case ATTACHMENT -> {
					var s = (String) value;
				}
				default -> throw new FieldValidationException("Неизвестный тип данных: %s.".formatted(dictField.getId()));
			}
		}
		catch (Exception e)
		{
			throw new FieldValidationException("Некорректный формат данных: %s.".formatted(dictField.getId()), e);
		}
	}

	private boolean checkStringLength(int length, Integer min, Integer max)
	{
		return (max != null && length > max) || (min != null && length < min);
	}

	private boolean checkNumberValue(Double value, Number min, Number max)
	{
		return (max != null && value > max.doubleValue()) || (min != null && value < min.doubleValue());
	}

	private boolean checkNumberValue(Long value, Integer min, Integer max)
	{
		return (max != null && value > max) || (min != null && value < min);
	}

	private void checkForbiddenField(Set<String> availableFieldIds, String fieldId)
	{
		if (ServiceFieldConstants.getServiceInsertableFields().contains(fieldId))
		{
			throw new ForbiddenFieldNameException(fieldId);
		}

		if (!availableFieldIds.contains(fieldId))
		{
			throw new ForbiddenFieldNameException(fieldId);
		}
	}

	private void validateRefDict(Dict scheme, List<DictFieldName> selectFields)
	{
		var availableRefDicts = scheme.getFields()
				.stream()
				.filter(it -> it.getType() == DictFieldType.DICT)
				.map(DictField::getDictRef)
				.map(DictFieldName::getDictId)
				.distinct()
				.map(dictService::getById)
				.collect(Collectors.toMap(Dict::getId, Function.identity()));

		selectFields.stream()
				.filter(it -> it.getDictId() != null)
				.filter(it -> !availableRefDicts.containsKey(it.getDictId()))
				.findAny()
				.ifPresent(it -> {
					throw new UnavailableDictRefException(it.getDictId());
				});

		var refFieldIds = selectFields.stream()
				.filter(it -> it.getDictId() != null)
				.map(DictFieldName::getFieldId)
				.filter(fieldId -> !"*".equals(fieldId))
				.collect(Collectors.toList());

		selectFields.stream()
				.filter(it -> it.getDictId() != null)
				.filter(it -> !"*".equals(it.getFieldId()))
				.map(it -> availableRefDicts.get(it.getDictId()))
				.forEach(it -> validateFieldsBySingleScheme(it, refFieldIds));
	}

	private void validateFieldsBySingleScheme(String dictId, List<String> selectFields)
	{
		var dict = dictService.getById(dictId);

		validateFieldsBySingleScheme(dict, selectFields);
	}

	private void validateFieldsBySingleScheme(Dict dict, List<String> selectFields)
	{
		var availableFieldIds = getAvailableFieldIds(dict.getFields());

		selectFields.stream()
				.filter(it -> !"*".equals(it))
				.filter(Predicate.not(availableFieldIds::contains))
				.findAny()
				.ifPresent(fieldId -> {
					throw new FieldNotFoundException(dict.getId(), fieldId);
				});
	}

	private Set<String> getAvailableFieldIds(List<DictField> fields)
	{
		return fields.stream()
				.map(DictField::getId)
				.collect(Collectors.toSet());
	}

	private void validateRelatedDictReferences(String dictId, Dict relatedDict)
	{
		var refFields = DictService.getReferenceFieldMap(relatedDict);

		var refDict = refFields.values()
				.stream()
				.filter(field -> field.getDictRef().getDictId().equals(dictId))
				.map(field -> dictDataService.existsByFilter(relatedDict.getId(), "%s != null".formatted(field.getId())))
				.findFirst()
				.orElse(null);

		if (refDict != null)
		{
			throw new DictException(
					"Невозможно удалить все записи из справочника '%s': на него есть ссылки в справочнике '%s'."
							.formatted(dictId, relatedDict.getId())
			);
		}
	}

	private boolean relatedItemExists(Dict dict, Dict relatedDict, String itemId)
	{
		return relatedDict.getFields()
				.stream()
				.filter(dictField -> dictField.getDictRef() != null)
				.filter(dictField -> dict.getId().equals(dictField.getDictRef().getDictId()))
				.anyMatch(dictField -> dictDataService.existsByFilter(relatedDict.getId(), buildQuery(dictField, itemId)));
	}

	private String buildQuery(DictField dictField, String itemId)
	{
		var operation = dictField.isMultivalued() ? " any['%s']" : " = '%s'";
		var query = dictField.getId() + operation;

		return query.formatted(itemId);
	}
}
