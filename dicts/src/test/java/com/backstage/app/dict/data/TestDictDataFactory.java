/*
 *    Copyright 2019-2025 the original author or authors.
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

package com.backstage.app.dict.data;

import com.backstage.app.dict.domain.DictItem;
import com.backstage.app.dict.model.dictitem.DictDataItem;
import com.backstage.app.dict.service.DictDataService;
import com.backstage.app.model.other.date.DateConstants;
import com.google.common.collect.ImmutableMap;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
public class TestDictDataFactory
{
	public static final String STRING_FIELD_VALUE = "string";

	public static final String STRING_FIELD = "stringField";
	public static final String INTEGER_FIELD = "integerField";
	public static final String DOUBLE_FIELD = "doubleField";
	public static final String TIMESTAMP_FIELD = "timestampField";
	public static final String BOOLEAN_FIELD = "booleanField";
	public static final String STRING_MULTIVALUED_FIELD = "stringFieldMultivalued";

	private final DictDataService dictDataService;

	protected static final Map<String, Object> RANDOM_DATA_MAP = ImmutableMap.of(
			STRING_FIELD, RandomStringUtils.randomAlphabetic(10),
			INTEGER_FIELD, RandomUtils.nextInt(0, 128),
			DOUBLE_FIELD, RandomUtils.nextDouble(0.0, 128.0),
			TIMESTAMP_FIELD, DateConstants.ISO_OFFSET_DATE_TIME_MS_FORMATTER.format(ZonedDateTime.now()),
			BOOLEAN_FIELD, RandomUtils.nextBoolean());

	protected static final Map<String, Object> DATA_MAP = Map.of(
			STRING_FIELD, STRING_FIELD_VALUE,
			INTEGER_FIELD, 1,
			DOUBLE_FIELD, 2.558,
			TIMESTAMP_FIELD, List.of("2021-08-15T06:00:00.000Z", "2021-08-15T08:00:00.000Z"),
			STRING_MULTIVALUED_FIELD, List.of("one", "two", "three"),
			BOOLEAN_FIELD, true);

	public List<DictItem> createManyWithDefaultValues(String dictId, int count)
	{
		var objects = new ArrayList<Map<String, Object>>();

		IntStream.range(0, count)
				.boxed()
				.forEach(i -> objects.add(DATA_MAP));

		return dictDataService.createMany(dictId, objects);
	}

	public List<DictItem> createManyWithRandomStringFieldValues(String dictId, String fieldId, int count)
	{
		var objects = new ArrayList<Map<String, Object>>();

		IntStream.range(0, count)
				.boxed()
				.forEach(i -> objects.add(Map.of(fieldId, fieldId + i)));

		return dictDataService.createMany(dictId, objects);
	}

	public DictItem createItemWithCustomFields(String dictId, Map<String, Object> customField)
	{
		return dictDataService.create(buildDictDataItem(dictId, customField));
	}

	public DictItem createDefaultItem(String dictId)
	{
		return dictDataService.create(buildDefaultDictDataItem(dictId));
	}

	public DictItem createDefaultItemWithCustomField(String dictId, Map<String, Object> customField)
	{
		var dataMap = new HashMap<>(DATA_MAP);
		dataMap.putAll(customField);

		return dictDataService.create(buildDictDataItem(dictId, dataMap));
	}

	//todo декомпозировать
	public List<DictItem> createDictHierarchy(String dictId, String refDictId, int count)
	{
		Supplier<Map<String, Object>> testDataMapFactory = () -> RANDOM_DATA_MAP;

		Function<String, Map<String, Object>> testRefDataMapFactory = (String id) ->
				ImmutableMap.<String, Object>builder()
						.putAll(testDataMapFactory.get())
						.put(dictId, id)
						.build();

		return IntStream.range(0, count)
				.boxed()
				.map(i -> testDataMapFactory.get())
				.map(dataMap -> dictDataService.create(buildDictDataItem(dictId, dataMap)).getId())
				.map(testRefDataMapFactory)
				.map(dataMap -> dictDataService.create(buildDictDataItem(refDictId, dataMap)))
				.toList();
	}

	public List<DictItem> createManyWithRandomValues(String dictId, int count)
	{
		Supplier<Map<String, Object>> testDataMapFactory = () -> RANDOM_DATA_MAP;

		return IntStream.range(0, count)
				.boxed()
				.map(i -> testDataMapFactory.get())
				.map(dataMap -> dictDataService.create(buildDictDataItem(dictId, dataMap)))
				.toList();
	}

	public DictDataItem buildDefaultDictDataItem(String dictId)
	{
		return DictDataItem.of(dictId, DATA_MAP);
	}

	public ArrayList<Map<String, Object>> buildManyDefaultDictDataItems(String dictId, int count)
	{
		var objects = new ArrayList<Map<String, Object>>();

		 IntStream.range(0, count)
				.boxed()
				.forEach(i -> objects.add(DATA_MAP));

		 return objects;
	}

	public DictDataItem buildDictDataItem(String dictId, Map<String, Object> dataMap)
	{
		return DictDataItem.of(dictId, dataMap);
	}
}

