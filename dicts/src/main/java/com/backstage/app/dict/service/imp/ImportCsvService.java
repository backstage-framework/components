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

package com.backstage.app.dict.service.imp;

import com.backstage.app.dict.api.domain.DictFieldType;
import com.backstage.app.dict.constant.ServiceFieldConstants;
import com.backstage.app.dict.domain.DictField;
import com.backstage.app.dict.domain.DictItem;
import com.backstage.app.dict.model.dictitem.DictDataItem;
import com.backstage.app.dict.service.DictDataService;
import com.backstage.app.dict.service.DictPermissionService;
import com.backstage.app.dict.service.DictService;
import com.backstage.app.dict.utils.CSVUtils;
import com.backstage.app.exception.AppException;
import com.backstage.app.model.other.exception.ApiStatusCodeImpl;
import com.backstage.app.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportCsvService implements ImportService
{
	private final DictService dictService;
	private final DictDataService dictDataService;
	private final DictPermissionService dictPermissionService;

	public List<DictItem> importDict(String dictId, InputStream inputStream)
	{
		return importDict(dictId, inputStream, SecurityUtils.getCurrentUserId());
	}

	public List<DictItem> importDict(String dictId, InputStream inputStream, String userId)
	{
		var dict = dictService.getById(dictId);
		var fieldMap = dict.getFields()
				.stream()
				.collect(Collectors.toMap(DictField::getId, Function.identity()));

		dictPermissionService.checkEditPermission(dict, userId);

		var result = new ArrayList<DictDataItem>();

		try (var reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8))
		{
//			TODO: валидация полей
			var format = CSVFormat.Builder.create(CSVFormat.EXCEL)
					.setDelimiter(',')
					.setHeader()
					.setSkipHeaderRecord(true)
					.build();

			var parser = format.parse(reader);

			var headerMap = parser.getHeaderMap()
					.entrySet()
					.stream()
					.collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

			for (var record : parser)
			{
				var recordMap = new LinkedHashMap<String, Object>();

				for (int i = 0; i < record.size(); i++)
				{
					if (ServiceFieldConstants.getServiceInsertableFields().contains(headerMap.get(i)))
					{
						continue;
					}

					var field = Optional.ofNullable(headerMap.get(i))
							.map(fieldMap::get)
							.orElseThrow();

					var parsedValue = parseValueByScheme(record.get(i), field);

					recordMap.put(headerMap.get(i), parsedValue);
				}

				result.add(DictDataItem.of(dictId, recordMap));
			}
		}
		catch (IOException e)
		{
			log.error("Ошибка при импорте справочника.", e);
		}

		return dictDataService.createMany(dictId, result, userId);
	}

	private Object parseValueByScheme(String stringValue, DictField field)
	{
		return field.isMultivalued()
				? parseMultiValueByScheme(stringValue, field.getType())
				: parseSingleValueByScheme(stringValue, field.getType());
	}

	private List<Object> parseMultiValueByScheme(String stringValue, DictFieldType targetType)
	{
		return Stream.of(CSVUtils.parseMultiValuedCell(stringValue))
				.map(it -> parseSingleValueByScheme(it, targetType))
				.filter(it -> filterEmptyAttachmentIds(it, targetType))
				.toList();
	}

	private Object parseSingleValueByScheme(String stringValue, DictFieldType targetType)
	{
		return switch (targetType)
		{
			case BOOLEAN -> Boolean.parseBoolean(stringValue);
			case INTEGER -> Long.parseLong(stringValue);
			case DECIMAL -> new BigDecimal(stringValue);
			case ATTACHMENT -> StringUtils.isBlank(stringValue) ? null : stringValue;
			case STRING, DICT, DATE, TIMESTAMP, JSON, ENUM, GEO_JSON -> stringValue;
			default -> throw new AppException(ApiStatusCodeImpl.ILLEGAL_INPUT,
					"Неизвестный тип поля: %s.".formatted(targetType));
		};
	}

	private boolean filterEmptyAttachmentIds(Object value, DictFieldType targetType)
	{
		if (value != null)
		{
			return true;
		}

		return targetType != DictFieldType.ATTACHMENT;
	}
}
