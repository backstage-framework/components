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
import com.backstage.app.dict.configuration.backend.provider.DictSchemeBackendProvider;
import com.backstage.app.dict.domain.Dict;
import com.backstage.app.dict.domain.DictField;
import com.backstage.app.dict.exception.EngineException;
import com.backstage.app.dict.exception.dict.DictException;
import com.backstage.app.dict.exception.dict.enums.EnumNotFoundException;
import com.backstage.app.dict.exception.dict.field.FieldValidationException;
import com.backstage.app.dict.service.DictService;
import com.backstage.app.utils.SpringContextUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class DictValidationService
{
	private static final int DICT_FIELD_ID_MAX_LENGTH = 32;

	private final DictSchemeBackendProvider schemeBackendProvider;

	private final Supplier<DictService> dictServiceSupplier = SpringContextUtils.createBeanSupplier(DictService.class);

	public void validateDrop(String dictId)
	{
		var relatedDictIds = dictServiceSupplier.get()
				.getAll()
				.stream()
				.filter(it -> isContainsReference(it, dictId))
				.map(Dict::getId)
				.toList();

		if (!relatedDictIds.isEmpty())
		{
			throw new DictException(
					"Невозможно удалить справочник '%s': в других справочниках присутствуют ссылки на него: %s."
							.formatted(dictId, String.join(", ", relatedDictIds))
			);
		}
	}

	public void validateDictScheme(Dict dict, DictService dictService)
	{
		validateDictEngine(dict);

		validateFields(dict, dictService);
	}

	private void validateDictEngine(Dict dict)
	{
		if (dict.getEngine() == null)
		{
			throw new EngineException("Значение engine для справочника '%s' не может быть null.".formatted(dict.getId()));
		}

		schemeBackendProvider.getBackendByEngineName(dict.getEngine().getName());
	}

	private void validateFields(Dict dict, DictService dictService)
	{
		dict.getFields()
				.forEach(field -> validateField(dict, field, dictService));
	}

	private void validateField(Dict dict, DictField field, DictService dictService)
	{
		if (field.getId().length() > DICT_FIELD_ID_MAX_LENGTH)
		{
			throw new FieldValidationException("Для поля %s превышена максимальная длина идентификатора в %s символов.".formatted(field.getId(), DICT_FIELD_ID_MAX_LENGTH));
		}
		else if (field.getType() == DictFieldType.DICT)
		{
			try
			{
				dictService.getById(field.getDictRef().getDictId());
			}
			catch (Exception e)
			{
				throw new FieldValidationException("Ошибка валидации поля-референса: %s.".formatted(field.getId()), e);
			}
		}
		else if (field.getType() == DictFieldType.ENUM)
		{
			dict.getEnums()
					.stream()
					.filter(it -> it.getId().equals(field.getEnumId()))
					.findAny()
					.orElseThrow(() -> new EnumNotFoundException(field.getEnumId()));
		}
		else if (field.getType() == DictFieldType.DECIMAL)
		{
			if (!checkDecimalMaxMinCondition(field))
			{
				throw new FieldValidationException("Значение minSize и maxSize для поля %s может быть только INTEGER или DECIMAL.".formatted(field.getId()));
			}
		}
		else if (field.getType() == DictFieldType.INTEGER || field.getType() == DictFieldType.STRING)
		{
			if (!checkIntegerMaxMinCondition(field))
			{
				throw new FieldValidationException("Значение minSize и maxSize для поля %s может быть только INTEGER.".formatted(field.getId()));
			}
		}

		if (field.getDefaultValue() != null)
		{
			// TODO: необходимо предварительно нормализовать defaultValue, а потом уже валидировать
//			dictDataValidationService.checkCast(dict.getId(), field,
//					field.getDefaultValue(), SecurityUtils.getCurrentUserId());
		}
	}

	private boolean checkDecimalMaxMinCondition(DictField field)
	{
		return (checkSingleElementType(field.getMinSize(), Integer.class) || checkSingleElementType(field.getMinSize(), Double.class))
				&& (checkSingleElementType(field.getMaxSize(), Integer.class) || checkSingleElementType(field.getMaxSize(), Double.class));
	}

	private boolean checkIntegerMaxMinCondition(DictField field)
	{
		return checkSingleElementType(field.getMinSize(), Integer.class) && checkSingleElementType(field.getMaxSize(), Integer.class);
	}

	private boolean checkSingleElementType(Object value, Class<?> clazz)
	{
		return value == null || value.getClass().equals(clazz);
	}

	private boolean isContainsReference(Dict dict, String relatedDictId)
	{
		return dict.getFields()
				.stream()
				.filter(it -> it.getDictRef() != null)
				.anyMatch(it -> relatedDictId.equals(it.getDictRef().getDictId()));
	}
}
