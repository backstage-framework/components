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

import com.backstage.app.attachment.model.domain.Attachment;
import com.backstage.app.attachment.service.AttachmentService;
import com.backstage.app.dict.api.domain.DictFieldType;
import com.backstage.app.dict.common.CommonTest;
import com.backstage.app.dict.constant.ServiceFieldConstants;
import com.backstage.app.dict.domain.Dict;
import com.backstage.app.dict.domain.DictField;
import com.backstage.app.dict.domain.DictFieldName;
import com.backstage.app.dict.domain.DictItem;
import com.backstage.app.dict.exception.dict.DictConcurrentUpdateException;
import com.backstage.app.dict.exception.dict.field.FieldNotFoundException;
import com.backstage.app.dict.exception.dict.field.FieldValidationException;
import com.backstage.app.dict.exception.dictitem.DictItemCreateException;
import com.backstage.app.dict.model.dictitem.DictDataItem;
import com.backstage.app.dict.service.advice.AttachmentDictDataServiceAdvice;
import com.backstage.app.exception.ObjectNotFoundException;
import com.backstage.app.model.other.date.DateConstants;
import com.backstage.app.model.other.user.UserInfo;
import com.backstage.app.utils.StreamCollectors;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Comparators;
import com.google.common.collect.ImmutableMap;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.assertj.core.api.ThrowingConsumer;
import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.GeoJsonObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.backstage.app.dict.constant.ServiceFieldConstants.ID;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CommonDictDataServiceTest extends CommonTest
{
	protected static String TESTABLE_DICT_ID;
	protected static String TESTABLE_REF_DICT_ID;
	protected static String TESTABLE_JSON_DICT_ID;
	protected static String TESTABLE_ATTACH_DICT_ID;
	protected static String TESTABLE_GEO_JSON_DICT_ID;
	protected static String TESTABLE_TRUNCATED_DICT_ID;
	protected static String TESTABLE_RESERVED_WORDS_DICT_ID;
	protected static String TESTABLE_SERIAL_TYPE_DICT_ID;

	@Autowired
	private AttachmentService attachmentService;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Value("classpath:attachment.png")
	protected Resource firstFileResource;

	@Value("classpath:attachment2.png")
	protected Resource secondFileResource;

	@Value("classpath:attachment3.png")
	protected Resource thirdFileResource;

	protected String firstAttachmentId;
	protected String secondAttachmentId;
	protected String thirdAttachmentId;

	protected Attachment firstAttachment;
	protected Attachment secondAttachment;
	protected Attachment thirdAttachment;

	protected static final Map<String, Object> DATA_MAP = Map.of(
			"stringField", "string",
			"integerField", 1,
			"doubleField", 2.558,
			"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T08:00:00.000Z"),
			"stringFieldMultivalued", List.of("one", "two", "three"),
			"booleanField", true);

	protected void initDictDataTestableHierarchy(String storageDictId)
	{
		TESTABLE_DICT_ID = createNewDict(storageDictId + "data").getId();

		var refDict = buildDict(storageDictId + "dataRef");

		refDict.getFields()
				.add(DictField.builder()
						.id(TESTABLE_DICT_ID)
						.name("Ссылка")
						.type(DictFieldType.DICT)
						.required(false)
						.multivalued(false)
						.dictRef(new DictFieldName(TESTABLE_DICT_ID, ID))
						.build());

		TESTABLE_REF_DICT_ID = dictService.create(refDict).getId();

		var attachDict = buildDict(storageDictId + "dataAttach");

		attachDict.getFields()
				.add(DictField.builder()
						.id("attachmentField")
						.name("Вложение")
						.type(DictFieldType.ATTACHMENT)
						.required(false)
						.multivalued(false)
						.build());

		attachDict.getFields()
				.add(DictField.builder()
						.id("attachmentsField")
						.name("Вложения")
						.type(DictFieldType.ATTACHMENT)
						.required(false)
						.multivalued(true)
						.build());

		TESTABLE_ATTACH_DICT_ID = dictService.create(attachDict).getId();

		var jsonDict = buildDict(storageDictId + "data_json");

		jsonDict.getFields()
				.add(DictField.builder()
						.id("jsonField")
						.name("Json")
						.type(DictFieldType.JSON)
						.required(true)
						.multivalued(false)
						.build());

		jsonDict.getFields()
				.add(DictField.builder()
						.id("jsonMultivaluedField")
						.name("Массив Json")
						.type(DictFieldType.JSON)
						.required(false)
						.multivalued(true)
						.build());

		TESTABLE_JSON_DICT_ID = dictService.create(jsonDict).getId();

		var geoJsonDict = buildDict(storageDictId + "data_geo_json");

		geoJsonDict.getFields()
				.add(DictField.builder()
						.id("geoJsonField")
						.name("GeoJson")
						.type(DictFieldType.GEO_JSON)
						.required(true)
						.multivalued(false)
						.build());

		geoJsonDict.getFields()
				.add(DictField.builder()
						.id("geoJsonMultivaluedField")
						.name("Массив GeoJson")
						.type(DictFieldType.GEO_JSON)
						.required(false)
						.multivalued(true)
						.build());

		TESTABLE_GEO_JSON_DICT_ID = dictService.create(geoJsonDict).getId();

		var truncatedDict = buildDict(storageDictId + "data_truncated");

		TESTABLE_TRUNCATED_DICT_ID = dictService.create(truncatedDict).getId();

		var reserverWordsDict = Dict.builder()
				.id((storageDictId + "ReserverWords"))
				.build();

		reserverWordsDict.getFields()
				.add(DictField.builder()
						.id("name")
						.type(DictFieldType.STRING)
						.required(true)
						.multivalued(false)
						.build());

		reserverWordsDict.getFields()
				.add(DictField.builder()
						.id("order")
						.type(DictFieldType.INTEGER)
						.required(false)
						.multivalued(false)
						.build());

		TESTABLE_RESERVED_WORDS_DICT_ID = dictService.create(reserverWordsDict).getId();

		var serialTypeDict = buildDict(storageDictId + "serial");

		serialTypeDict.getFields()
				.add(DictField.builder()
						.id("serialField")
						.name("Поле с автоинкрементом")
						.type(DictFieldType.SERIAL)
						.required(true)
						.build());

		TESTABLE_SERIAL_TYPE_DICT_ID = dictService.create(serialTypeDict).getId();
	}

	@BeforeAll
	public void setupAttachment() throws IOException
	{
		firstAttachment = createAttachment(firstFileResource);
		secondAttachment = createAttachment(secondFileResource);
		thirdAttachment = createAttachment(thirdFileResource);

		firstAttachmentId = firstAttachment.getId();
		secondAttachmentId = secondAttachment.getId();
		thirdAttachmentId = thirdAttachment.getId();
	}

	private final ThrowingConsumer<Object> objectWriter = obj -> System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj));

	protected void getByIds()
	{
		var ids = dictDataService.createMany(TESTABLE_DICT_ID, List.of(DATA_MAP, DATA_MAP))
				.stream()
				.map(DictItem::getId)
				.toList();

		assertArrayEquals(ids.toArray(String[]::new), dictDataService.getByIds(TESTABLE_DICT_ID, ids).stream().map(DictItem::getId).toArray(String[]::new));
	}

	protected void getDistinctValuesByFilterWithEmptyFilter()
	{
		var result = dictDataService.getDistinctValuesByFilter(TESTABLE_REF_DICT_ID, TESTABLE_DICT_ID + ".stringField", "");

		assertEquals(12, result.size());
	}

	protected void getDistinctValuesByFilterWithFilter()
	{
		var result = dictDataService.getDistinctValuesByFilter(TESTABLE_REF_DICT_ID, TESTABLE_DICT_ID + ".stringField", "integerField != null");

		assertEquals(6, result.size());
	}

	protected void streamByFilter()
	{
		var query = "stringField like 'string'";

		try (var stream = dictDataService.streamByFilter(TESTABLE_REF_DICT_ID, List.of("*"), query))
		{
			var itemCount = dictDataService.getByFilter(TESTABLE_REF_DICT_ID, List.of("*"), query, PageRequest.of(0, 10)).getTotalElements();

			assertEquals(itemCount, stream.count());
		}
	}

	protected void checkSerialField()
	{
		int count = RandomUtils.secure().randomInt(10, 30);

		for (int i = 0; i < count; i++)
		{
			var item = dictDataService.create(DictDataItem.of(TESTABLE_SERIAL_TYPE_DICT_ID, DATA_MAP));

			assertEquals(i + 1L, item.getData().get("serialField"));
		}

		dictService.restartSerialField(TESTABLE_SERIAL_TYPE_DICT_ID, "serialField", 1L);

		for (int i = 0; i < count; i++)
		{
			var item = dictDataService.create(DictDataItem.of(TESTABLE_SERIAL_TYPE_DICT_ID, DATA_MAP));

			assertEquals(i + 1L, item.getData().get("serialField"));
		}

		dictService.renameField(TESTABLE_SERIAL_TYPE_DICT_ID, "serialField", "serialField2", null);

		for (int i = count; i < count + RandomUtils.secure().randomLong(10, 30); i++)
		{
			var item = dictDataService.create(DictDataItem.of(TESTABLE_SERIAL_TYPE_DICT_ID, DATA_MAP));

			assertEquals(i + 1L, item.getData().get("serialField2"));
		}

		var dict = dictService.getById(TESTABLE_SERIAL_TYPE_DICT_ID).copy();
		dict.getFields().add(DictField.builder()
				.id("serialField")
				.type(DictFieldType.SERIAL)
				.build());

		dictService.update(TESTABLE_SERIAL_TYPE_DICT_ID, dict);

		for (int i = 0; i < count; i++)
		{
			var item = dictDataService.create(DictDataItem.of(TESTABLE_SERIAL_TYPE_DICT_ID, DATA_MAP));

			assertEquals(i + 1L, item.getData().get("serialField"));
		}
	}

	protected void checkReserverWords()
	{
		var name = "testName";
		var order = RandomUtils.secure().randomLong();

		var item = dictDataService.create(DictDataItem.of(TESTABLE_RESERVED_WORDS_DICT_ID, Map.of(
				"name", name,
				"order", order
		)));

		assertEquals(name, item.getData().get("name"));
		assertEquals(order, item.getData().get("order"));

		order = RandomUtils.secure().randomLong();

		item = dictDataService.update(item.getId(), DictDataItem.of(TESTABLE_RESERVED_WORDS_DICT_ID, Map.of(
				"name", name,
				"order", order
		)), item.getVersion());

		assertEquals(name, item.getData().get("name"));
		assertEquals(order, item.getData().get("order"));
	}

	protected void getByFilterWithNullRefField()
	{
		var dataMap = new HashMap<>(DATA_MAP);
		dataMap.put("stringField", "nullableDictRefField");

		var createdItemId = dictDataService.create(buildDictDataItem(TESTABLE_REF_DICT_ID, dataMap))
				.getId();

		var result = dictDataService.getByFilter(TESTABLE_REF_DICT_ID, List.of("*", TESTABLE_DICT_ID + ".*"), "stringField = 'nullableDictRefField'", PageRequest.of(0, 10));

		assertEquals(result.getContent().size(), 1);
		assertEquals(result.getContent().get(0).getId(), createdItemId);
	}

	protected void getByFilter()
	{
		var result = dictDataService.getByFilter(TESTABLE_DICT_ID, List.of("*"),
				"stringField like 'string' and (integerField = 1 or integerField in (2, 5, 8) and integerField != 10 or integerField <= 2 and doubleField > 1.9 and doubleField < 2.1)",
				PageRequest.of(0, 9));

		var allMatchStringField = result.getContent()
				.stream()
				.map(DictItem::getData)
				.map(Map::entrySet)
				.flatMap(Collection::stream)
				.filter(it -> "stringField".equals(it.getKey()))
				.map(Map.Entry::getValue)
				.allMatch(DATA_MAP.get("stringField")::equals);

		assertFalse(result.getContent().isEmpty());
		assertTrue(allMatchStringField);
	}

	protected void getByFilterWithPrefixLikeExpression()
	{
		var queryValue = "str";

		var result = dictDataService.getByFilter(TESTABLE_DICT_ID, List.of("*"),
				"stringField like '%s%%' and stringField ilike '%s%%'".formatted(queryValue, queryValue.toUpperCase()),
				PageRequest.of(0, 10));

		var allMatchStringField = result.getContent()
				.stream()
				.map(DictItem::getData)
				.map(Map::entrySet)
				.flatMap(Collection::stream)
				.filter(it -> "stringField".equals(it.getKey()))
				.map(Map.Entry::getValue)
				.allMatch(it -> ((String) it).startsWith(queryValue));

		assertFalse(result.getContent().isEmpty());
		assertTrue(allMatchStringField);
	}

	protected void getByFilterWithInnerLikeExpression()
	{
		var queryValue = "rin";

		var result = dictDataService.getByFilter(TESTABLE_DICT_ID, List.of("*"),
				"stringField like '%%%s%%' and stringField ilike '%%%s%%'".formatted(queryValue, queryValue.toUpperCase()),
				PageRequest.of(0, 10));

		var allMatchStringField = result.getContent()
				.stream()
				.map(DictItem::getData)
				.map(Map::entrySet)
				.flatMap(Collection::stream)
				.filter(it -> "stringField".equals(it.getKey()))
				.map(Map.Entry::getValue)
				.allMatch(it -> ((String) it).contains(queryValue));

		assertFalse(result.getContent().isEmpty());
		assertTrue(allMatchStringField);
	}

	protected void getByFilterWithPostfixLikeExpression()
	{
		var queryValue = "ring";

		var result = dictDataService.getByFilter(TESTABLE_DICT_ID, List.of("*"),
				"stringField like '%%%s' and stringField ilike '%%%s'".formatted(queryValue, queryValue.toUpperCase()),
				PageRequest.of(0, 9));

		var allMatchStringField = result.getContent()
				.stream()
				.map(DictItem::getData)
				.map(Map::entrySet)
				.flatMap(Collection::stream)
				.filter(it -> "stringField".equals(it.getKey()))
				.map(Map.Entry::getValue)
				.allMatch(it -> ((String) it).endsWith(queryValue));

		assertFalse(result.getContent().isEmpty());
		assertTrue(allMatchStringField);
	}

	protected void getByFilterWithUnderscoreLikeExpression()
	{
		var result = dictDataService.getByFilter(TESTABLE_DICT_ID, List.of("*"),
				"stringField like 'strin_' and stringField ilike 'STRIN_'",
				PageRequest.of(0, 10));

		var allMatchStringField = result.getContent()
				.stream()
				.map(DictItem::getData)
				.map(Map::entrySet)
				.flatMap(Collection::stream)
				.filter(it -> "stringField".equals(it.getKey()))
				.map(Map.Entry::getValue)
				.allMatch(DATA_MAP.get("stringField")::equals);

		assertFalse(result.getContent().isEmpty());
		assertTrue(allMatchStringField);
	}

	protected void getByFilterWithEscapeLikeSpecialSymbols()
	{
		var dataMap = new HashMap<>(DATA_MAP);
		dataMap.put("stringField", "st%i_g");

		var dictItemId = dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, dataMap))
				.getId();

		var result = dictDataService.getByFilter(TESTABLE_DICT_ID, List.of("*"),
				"stringField like 'st\\%i\\_g' and stringField ilike 'ST\\%I\\_G'",
				PageRequest.of(0, 10));

		assertEquals(result.getContent().size(), 1);
		assertEquals(result.getContent().get(0).getId(), dictItemId);
	}

	protected void getByFilterWithLogicalExpression()
	{
		var dataMap = new HashMap<>(DATA_MAP);
		dataMap.put("stringField", "stringFieldLogicalExpressionTest");

		var dictItemId = dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, dataMap))
				.getId();

		var result = dictDataService.getByFilter(TESTABLE_DICT_ID, List.of("*"),
				"stringField = 'stringFieldLogicalExpressionTest' and (doubleField = 2.558 or integerField = 1)",
				PageRequest.of(0, 10));

		assertEquals(1, result.getContent().size());
		assertEquals(result.getContent().get(0).getId(), dictItemId);
	}

	protected void getIdsByFilter()
	{
		var result = dictDataService.getIdsByFilter(TESTABLE_DICT_ID, "integerField = 1 or stringField like 'string' or integerField in (2, 5, 8) and integerField != 10 or integerField <= 2 and doubleField > 1.9 and doubleField < 2.1");

		assertNotNull(result.getContent().get(0));
	}

	//TODO: тест - с ambiguous состоянием указанных поле в сортировке
	protected void getByFilterInnerDictSort()
	{
		final String integerField = createDictHierarchy();

		var result = dictDataService.getByFilter(TESTABLE_REF_DICT_ID, List.of("*", TESTABLE_DICT_ID + ".*"), "%s != null".formatted(TESTABLE_DICT_ID),
						PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, TESTABLE_DICT_ID + "." + integerField)))
				.getContent()
				.stream()
				.map(DictItem::getData)
				.toList();

		objectWriter.accept(result);

		boolean actual = Comparators.isInOrder(result, Comparator.comparing((Map<String, Object> data) -> (Long) ((DictItem) data.get(TESTABLE_DICT_ID)).getData().get(integerField)).reversed());

		assertTrue(actual);
	}

	protected void getByFilterInnerDictSortWrongFiledName()
	{
		assertThrows(FieldNotFoundException.class, () -> dictDataService.getByFilter(TESTABLE_REF_DICT_ID, List.of("*", TESTABLE_DICT_ID + ".*"), null,
				PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, TESTABLE_DICT_ID + "." + "wrongFiled"))));
	}

	protected void getByFilterDictSortServiceField()
	{
		createDictHierarchy();

		var sortedIdFields = dictDataService.getByFilter(TESTABLE_DICT_ID, List.of("*"), null,
						PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, ServiceFieldConstants.ID)))
				.getContent()
				.stream()
				.map(DictItem::getId)
				.toList();

		var sortedCreatedFields = dictDataService.getByFilter(TESTABLE_DICT_ID, List.of("*"), null,
						PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, ServiceFieldConstants.CREATED)))
				.getContent()
				.stream()
				.map(DictItem::getCreated)
				.toList();

		var actualIdFields = Comparators.isInOrder(sortedIdFields, Comparator.reverseOrder());
		var actualCreatedFields = Comparators.isInOrder(sortedCreatedFields, Comparator.reverseOrder());

		assertTrue(actualIdFields);
		assertTrue(actualCreatedFields);
	}

	//todo убрать передачу ожидаемого значения кол-ва элементов после фикса пагинации для MongoDB
	protected void getByFilterWithUnpagedSort(int totalElements)
	{
		var integerField = createDictHierarchy();

		var result = dictDataService.getByFilter(TESTABLE_DICT_ID, List.of("*"), null,
						Pageable.unpaged(Sort.by(Sort.Direction.DESC, integerField)))
				.getContent()
				.stream()
				.map(DictItem::getData)
				.map(map -> (Long) map.get(integerField))
				.toList();

		assertEquals(totalElements, result.size());

		var actual = Comparators.isInOrder(result, Comparator.reverseOrder());

		assertTrue(actual);
	}

	protected void getByFilterUnpaged()
	{
		var result = dictDataService.getByFilter(TESTABLE_DICT_ID, List.of("*"), null, Pageable.unpaged());

		assertTrue(result.getPageable().isUnpaged());
		assertEquals(1, result.getTotalPages());
	}

	protected void getByFilterDictSortDataField()
	{
		var sortedDataField = createDictHierarchy();

		var result = dictDataService.getByFilter(TESTABLE_DICT_ID, List.of("*"), null,
						PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, sortedDataField)))
				.getContent()
				.stream()
				.map(DictItem::getData)
				.map(map -> (Long) map.get(sortedDataField))
				.toList();

		var actual = Comparators.isInOrder(result, Comparator.reverseOrder());

		assertTrue(actual);
	}

	protected void getByFilterWithMultipleSortDataField()
	{
		var result = dictDataService.getByFilter(TESTABLE_DICT_ID, List.of("*"), "integerField != null and doubleField != null and id != 'with_id'", PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "integerField", "doubleField")))
				.getContent();

		var resultSortedCorrectly = Comparators.isInOrder(result, Comparator.comparing(dictItem -> (Long) ((DictItem) dictItem).getData().get("integerField"))
				.thenComparing(dictItem -> (BigDecimal) ((DictItem) dictItem).getData().get("doubleField")));

		assertTrue(resultSortedCorrectly);
	}

	protected void getByFilterWithServiceSelectField()
	{
		createDictHierarchy();

		var result = dictDataService.getByFilter(TESTABLE_DICT_ID, List.of(ServiceFieldConstants.CREATED),
						null, PageRequest.of(0, 20))
				.getContent()
				.stream()
				.map(it -> StringUtils.hasText(it.getId()) && it.getCreated() != null && it.getVersion() != null)
				.toList();

		result.forEach(Assertions::assertTrue);
	}

	protected void getByFilterWithDifferentDateCorrect()
	{
		var localDateTime = LocalDateTime.of(2021, 8, 15, 6, 0, 0);
		var date = Date.from(LocalDateTime.of(2021, 8, 15, 6, 0, 0).atZone(ZoneId.systemDefault()).toInstant());

		Map<String, Object> stringLocalDateTimeMap = Map.of(
				"stringField", "string",
				"integerField", 1.0,
				"doubleField", 2.50,
				"timestampField", "2021-08-15T06:00:00.000Z",
				"booleanField", Boolean.TRUE);

		Map<String, Object> objectLocalDateTimeMap = Map.of(
				"stringField", "string",
				"integerField", 1.0,
				"doubleField", 2.50,
				"timestampField", localDateTime,
				"booleanField", Boolean.FALSE);

		Map<String, Object> stringDateDataMap = Map.of(
				"stringField", "string",
				"integerField", 1.0,
				"doubleField", 2.50,
				"timestampField", "2021-08-15T06:00:00.000Z",
				"booleanField", Boolean.TRUE);

		Map<String, Object> objectDateDataMap = Map.of(
				"stringField", "string",
				"integerField", 1.0,
				"doubleField", 2.50,
				"timestampField", date,
				"booleanField", Boolean.FALSE);

		var stringDateItemId = dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, stringDateDataMap)).getId();
		var objectDateItemId = dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, objectDateDataMap)).getId();
		var stringLocalDateTimeItemId = dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, stringLocalDateTimeMap)).getId();
		var objectLocalDateTimeItemId = dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, objectLocalDateTimeMap)).getId();

		var stringDateItem = dictDataService.getById(TESTABLE_DICT_ID, stringDateItemId);
		var objectDateItem = dictDataService.getById(TESTABLE_DICT_ID, objectDateItemId);
		var stringLocalDateTimeItem = dictDataService.getById(TESTABLE_DICT_ID, stringLocalDateTimeItemId);
		var objectLocalDateTimeItem = dictDataService.getById(TESTABLE_DICT_ID, objectLocalDateTimeItemId);

		assertEquals(stringDateItem.getData().get("timestampField"), List.of(localDateTime));
		assertEquals(objectDateItem.getData().get("timestampField"), List.of(localDateTime));
		assertEquals(stringLocalDateTimeItem.getData().get("timestampField"), List.of(localDateTime));
		assertEquals(objectLocalDateTimeItem.getData().get("timestampField"), List.of(localDateTime));
	}

	//TODO: тест с выбором всех полей у refDict
	protected void getByFilterWithDictReference()
	{
		var refId = dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, DATA_MAP)).getId();

		var refDataMap = new HashMap<>(DATA_MAP);
		refDataMap.put(TESTABLE_DICT_ID, refId);

		dictDataService.create(buildDictDataItem(TESTABLE_REF_DICT_ID, refDataMap));

		var result = dictDataService.getByFilter(TESTABLE_REF_DICT_ID, List.of("*", TESTABLE_DICT_ID + ".timestampField"), null, PageRequest.of(0, 10));

		assertNotNull(result.getContent().get(0).getData().get(TESTABLE_DICT_ID));
		assertEquals(DictItem.class, result.getContent().get(0).getData().get(TESTABLE_DICT_ID).getClass());
		assertTrue(((DictItem) result.getContent().get(0).getData().get(TESTABLE_DICT_ID)).getData().containsKey("timestampField"));
	}

	protected void getByFilterWithDictReferenceAllFieldSelect()
	{
		var refId = dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, DATA_MAP)).getId();

		var refDataMap = new HashMap<>(DATA_MAP);
		refDataMap.put(TESTABLE_DICT_ID, refId);

		var createdDitDataItemId = dictDataService.create(buildDictDataItem(TESTABLE_REF_DICT_ID, refDataMap))
				.getId();

		var result = dictDataService.getByFilter(TESTABLE_REF_DICT_ID, List.of("*"), "%s = '%s'".formatted(TESTABLE_DICT_ID, refId), PageRequest.of(0, 10))
				.getContent();

		assertEquals(1, result.size());
		assertEquals(result.get(0).getId(), createdDitDataItemId);
		assertEquals(result.get(0).getData().size(), refDataMap.size());
		assertEquals(result.get(0).getData().get(TESTABLE_DICT_ID), refId);
	}

	//TODO: тест с фильтрацией reference Dict по элементам массива
	protected void getByFilterWithQueryReference()
	{
		var refDictDataMap = new HashMap<>(DATA_MAP);
		refDictDataMap.put("stringField", "queryReferenceDict");

		var refId = dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, refDictDataMap)).getId();

		var dataMap = new HashMap<>(DATA_MAP);
		dataMap.put(TESTABLE_DICT_ID, refId);

		dictDataService.create(buildDictDataItem(TESTABLE_REF_DICT_ID, dataMap));

		var query = "integerField = 1 and %s.stringField = 'queryReferenceDict'".formatted(TESTABLE_DICT_ID);

		var actual = dictDataService.getByFilter(TESTABLE_REF_DICT_ID, List.of("*", TESTABLE_DICT_ID + ".stringField"), query, Pageable.unpaged())
				.getContent()
				.stream()
				.map(DictItem::getData)
				.map(Map::entrySet)
				.flatMap(Collection::stream)
				.filter(it -> TESTABLE_DICT_ID.equals(it.getKey()))
				.map(Map.Entry::getValue)
				.map(it -> (DictItem) it)
				.findFirst()
				.orElseThrow(() -> new RuntimeException("Query reference test failed."));

		assertEquals(refId, actual.getId());
		assertEquals(refDictDataMap.get("stringField"), actual.getData().get("stringField"));
	}

	protected void getByFilterWithArrayContainsAnyValue()
	{
		var dataMap = new HashMap<>(DATA_MAP);
		dataMap.put("timestampField", List.of("2021-08-15T06:00:00.000Z", "2023-04-15T06:00:00.000Z", "2019-08-15T06:00:00.000Z"));
		dataMap.put("stringFieldMultivalued", List.of("one", "two", "three"));

		var dictItem = dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, dataMap));

		var anyQuery = "timestampField any ['2021-08-15T06:00:00'::timestamp, '2001-08-16T08:00:00'::timestamp, '2006-08-16T08:00:00'::timestamp] and stringFieldMultivalued any ['one']";

		var actualIds = dictDataService.getByFilter(TESTABLE_DICT_ID, List.of("*"), anyQuery, Pageable.unpaged())
				.getContent()
				.stream()
				.map(DictItem::getId)
				.collect(Collectors.toSet());

		assertTrue(actualIds.contains(dictItem.getId()));
	}

	protected void getByFilterWithArrayContainsAllValue()
	{
		var dataMap = new HashMap<>(DATA_MAP);
		dataMap.put("timestampField", List.of("2021-08-15T06:00:00.000Z", "2001-08-16T08:00:00.000Z", "2006-08-16T08:00:00.000Z"));
		dataMap.put("stringFieldMultivalued", List.of("one", "two", "three"));

		var dictItem = dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, dataMap));

		var allQuery = "timestampField all ['2021-08-15T06:00:00'::timestamp, '2001-08-16T08:00:00'::timestamp, '2006-08-16T08:00:00'::timestamp] and stringFieldMultivalued all ['one', 'two', 'three']";

		var actualIds = dictDataService.getByFilter(TESTABLE_DICT_ID, List.of("*"), allQuery, Pageable.unpaged())
				.getContent()
				.stream()
				.map(DictItem::getId)
				.collect(Collectors.toSet());

		assertTrue(actualIds.contains(dictItem.getId()));
	}

	protected void existsById()
	{
		var item = dictDataService.getByFilter(TESTABLE_DICT_ID, List.of("*"), "integerField = 1", PageRequest.of(0, 1))
				.toList()
				.get(0);

		assertTrue(dictDataService.existsById(TESTABLE_DICT_ID, item.getId()));

		assertFalse(dictDataService.existsById(TESTABLE_DICT_ID, "-1"));
	}

	protected void existsByFilter()
	{
		assertTrue(dictDataService.existsByFilter(TESTABLE_DICT_ID, "integerField = 1"));

		assertFalse(dictDataService.existsByFilter(TESTABLE_DICT_ID, "integerField = 132"));
	}

	protected void attachmentBindingWithCreateDictItem()
	{
		var attachmentDataMap = Map.of(
				"stringField", "string",
				"integerField", 1L,
				"doubleField", 2,
				"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T08:00:00.000Z"),
				"attachmentField", firstAttachmentId,
				"attachmentsField", List.of(firstAttachmentId),
				"booleanField", true);

		var dictItem = dictDataService.create(buildDictDataItem(TESTABLE_ATTACH_DICT_ID, attachmentDataMap));

		var singleFieldAttachments = attachmentService.getAttachments(AttachmentDictDataServiceAdvice.DICT_ITEM_ATTACHMENT_TYPE, AttachmentDictDataServiceAdvice.getAttachmentOwnerId(TESTABLE_ATTACH_DICT_ID, dictItem));
		var multivaluedFieldAttachments = attachmentService.getAttachments(AttachmentDictDataServiceAdvice.DICT_ITEM_ATTACHMENT_TYPE, AttachmentDictDataServiceAdvice.getAttachmentOwnerId(TESTABLE_ATTACH_DICT_ID, dictItem));

		assertNotNull(dictItem);
		assertEquals(firstAttachment.getId(), singleFieldAttachments.get(0).getId());
		assertEquals(firstAttachment.getId(), multivaluedFieldAttachments.get(0).getId());
	}

	protected void checkAttachmentBindingWithUpdateDictItem()
	{
		var attachmentDataMap = Map.of(
				"stringField", "string",
				"integerField", 1L,
				"doubleField", 2,
				"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T08:00:00.000Z"),
				"attachmentsField", List.of(secondAttachmentId, thirdAttachmentId),
				"booleanField", true);

		var attachmentDataUpdateMap = Map.of(
				"stringField", "string",
				"integerField", 1L,
				"doubleField", 2,
				"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T08:00:00.000Z"),
				"attachmentField", secondAttachmentId,
				"attachmentsField", List.of(secondAttachmentId, thirdAttachmentId),
				"booleanField", false);

		var dictItem = dictDataService.create(buildDictDataItem(TESTABLE_ATTACH_DICT_ID, attachmentDataMap));
		var updatedDictItem = dictDataService.update(dictItem.getId(), buildDictDataItem(TESTABLE_ATTACH_DICT_ID, attachmentDataUpdateMap), dictItem.getVersion());

		var fieldAttachments = attachmentService.getAttachments(AttachmentDictDataServiceAdvice.DICT_ITEM_ATTACHMENT_TYPE, AttachmentDictDataServiceAdvice.getAttachmentOwnerId(TESTABLE_ATTACH_DICT_ID, updatedDictItem));
		var fieldAttachmentsIds = fieldAttachments.stream().map(Attachment::getId).toList();

		assertNotNull(dictItem);
		assertTrue(fieldAttachmentsIds.contains(secondAttachmentId));
		assertTrue(fieldAttachmentsIds.contains(thirdAttachmentId));
		assertEquals(2, fieldAttachmentsIds.size());
	}

	protected void checkAttachmentReleaseWithDeleteDictItem()
	{
		var attachmentDataMap = Map.of(
				"stringField", "string",
				"integerField", 1L,
				"doubleField", 2,
				"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T08:00:00.000Z"),
				"booleanField", true,
				"attachmentField", firstAttachmentId,
				"attachmentsField", List.of(firstAttachmentId));

		var dictItem = dictDataService.create(buildDictDataItem(TESTABLE_ATTACH_DICT_ID, attachmentDataMap));
		var dictItemAttachments = attachmentService.getAttachments(AttachmentDictDataServiceAdvice.DICT_ITEM_ATTACHMENT_TYPE, AttachmentDictDataServiceAdvice.getAttachmentOwnerId(TESTABLE_ATTACH_DICT_ID, dictItem));

		assertFalse(dictItemAttachments.isEmpty());
		assertEquals(firstAttachmentId, dictItemAttachments.get(0).getId());

		dictDataService.delete(TESTABLE_ATTACH_DICT_ID, dictItem.getId());

		dictItemAttachments = attachmentService.getAttachments(AttachmentDictDataServiceAdvice.DICT_ITEM_ATTACHMENT_TYPE, AttachmentDictDataServiceAdvice.getAttachmentOwnerId(TESTABLE_ATTACH_DICT_ID, dictItem));

		assertTrue(dictItemAttachments.isEmpty());
	}

	protected void createDictItem()
	{
		assertNotNull(dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, DATA_MAP)));
	}

	protected void createManyDictItems()
	{
		assertNotNull(dictDataService.createMany(TESTABLE_DICT_ID, List.of(DATA_MAP, DATA_MAP, DATA_MAP)));
	}

	protected void createDictItemWithNullData()
	{
		var dataMap = new HashMap<>(DATA_MAP);
		dataMap.put("doubleField", null);

		dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, dataMap));

		var actual = dictDataService.getByFilter(TESTABLE_DICT_ID, List.of("*"), "doubleField = null", Pageable.unpaged())
				.getContent()
				.stream()
				.map(DictItem::getData)
				.map(Map::entrySet)
				.flatMap(Collection::stream)
				.filter(it -> "doubleField".equals(it.getKey()))
				.collect(StreamCollectors.toLinkedHashMap(Map.Entry::getKey, Map.Entry::getValue));

		actual.values().forEach(Assertions::assertNull);
	}

	protected void createWithDifferentType()
	{
		var longDataMap = Map.of(
				"stringField", "string",
				"integerField", 1L,
				"doubleField", 2,
				"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T08:00:00.000Z"),
				"booleanField", true);

		var doubleDataMap = Map.of(
				"stringField", "string",
				"integerField", 1.0,
				"doubleField", 2.50,
				"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T08:00:00.000Z"),
				"booleanField", Boolean.TRUE);

		assertNotNull(dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, DATA_MAP)));
		assertNotNull(dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, longDataMap)));
		assertNotNull(dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, doubleDataMap)));
	}

	protected void createWithUUIDDictFieldStartNumericIds()
	{
		var fields = new ArrayList<DictField>();
		var fieldId = RandomStringUtils.randomNumeric(1) + org.apache.commons.lang3.StringUtils.substring(generateRandomUUIDWithoutDashes(), 1);

		var dictFieldStartNumeric = DictField.builder()
				.id(fieldId)
				.name("строка")
				.type(DictFieldType.STRING)
				.required(true)
				.multivalued(false)
				.build();

		fields.add(dictFieldStartNumeric);

		var dict = buildDict(RandomStringUtils.randomNumeric(1) + org.apache.commons.lang3.StringUtils.substring(generateRandomUUIDWithoutDashes(), 1));
		dict.setFields(fields);
		dict.setIndexes(List.of(buildIndex(dict.getId(), dict.getFieldIds().getFirst())));

		var dictId = dictService.create(dict)
				.getId();

		var dictDataItem = buildDictDataItem(dictId, Map.of(fieldId, "1__numericStringValue"));
		var dictItem = dictDataService.create(dictDataItem);

		assertEquals(dictItem.getData().get(fieldId), dictDataItem.getDataItemMap().get(fieldId));

		var updatedDictDataItem = buildDictDataItem(dictId, Map.of(fieldId, "stringValue"));
		var updatedDictItem = dictDataService.update(dictItem.getId(), updatedDictDataItem, dictItem.getVersion());

		assertEquals(updatedDictItem.getData().get(fieldId), updatedDictDataItem.getDataItemMap().get(fieldId));
	}

	protected void checkHistoryMaxSize()
	{
		var dataMap = new HashMap<>(DATA_MAP);
		var dictItem = dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, dataMap));

		assertNotNull(dictItem.getHistory());
		assertTrue(dictItem.getHistory().isEmpty());

		var historyLimit = 10;

		var dict = dictService.getById(TESTABLE_DICT_ID).copy();
		dict.setMaxHistory(historyLimit);

		dict = dictService.update(TESTABLE_DICT_ID, dict);

		var targetFieldName = "integerField";
		var counter = 1000;

		for (int i = 0; i < historyLimit + 5; i++)
		{
			dataMap = new HashMap<>(dictItem.getData());
			dataMap.put(targetFieldName, counter++);

			dictItem = dictDataService.update(dictItem.getId(), buildDictDataItem(TESTABLE_DICT_ID, dataMap), dictItem.getVersion());
		}

		assertEquals(historyLimit, dictItem.getHistory().size());
		assertEquals((Long) dictItem.getData().get(targetFieldName) - 1, ((Integer) dictItem.getHistory().get(9).get(targetFieldName)).longValue());

		dict.setMaxHistory(null);

		for (int i = 0; i < historyLimit + 5; i++)
		{
			dataMap = new HashMap<>(dictItem.getData());
			dataMap.put(targetFieldName, counter++);

			dictItem = dictDataService.update(dictItem.getId(), buildDictDataItem(TESTABLE_DICT_ID, dataMap), dictItem.getVersion());
		}

		assertEquals(2 * historyLimit + 5, dictItem.getHistory().size());
	}

	protected void createCorrectContainsFieldsInHistoryMap()
	{
		var itemId = "item_id";

		var dataMap = new HashMap<>(DATA_MAP);
		dataMap.put(ServiceFieldConstants.ID, itemId);

		var dictItem = dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, dataMap));

		assertNotNull(dictItem.getHistory());
		assertTrue(dictItem.getHistory().isEmpty());

		var targetFieldName = "doubleField";

		dataMap = new HashMap<>(DATA_MAP);
		dataMap.put(targetFieldName, null);

		dictItem = dictDataService.update(itemId, buildDictDataItem(TESTABLE_DICT_ID, dataMap), dictItem.getVersion());

		assertNull(dictItem.getData().get(targetFieldName));
		assertNotNull(dictItem.getHistory());
		assertFalse(dictItem.getHistory().isEmpty());

		assertEquals(DATA_MAP.get(targetFieldName), dictItem.getHistory().get(0).get(targetFieldName));
	}

	protected void createCorrectContainsFieldsInHistoryMapForSkippedNullValue()
	{
		var itemId = "skipped_null_item_id";

		var dataMap = new HashMap<>(DATA_MAP);
		dataMap.put(ServiceFieldConstants.ID, itemId);

		var dictItem = dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, dataMap));

		assertNotNull(dictItem.getHistory());
		assertTrue(dictItem.getHistory().isEmpty());

		var targetFieldName = "doubleField";

		dataMap = new HashMap<>(DATA_MAP);
		dataMap.remove(targetFieldName);

		dictItem = dictDataService.update(itemId, buildDictDataItem(TESTABLE_DICT_ID, dataMap), dictItem.getVersion());

		assertNull(dictItem.getData().get(targetFieldName));
		assertNotNull(dictItem.getHistory());
		assertFalse(dictItem.getHistory().isEmpty());

		assertEquals(DATA_MAP.get(targetFieldName), dictItem.getHistory().get(0).get(targetFieldName));
	}

	//TODO: расширить тест когда Json указывается строкой
	protected void createDictItemWithJson()
	{
		var dataMap = new HashMap<>(DATA_MAP);

		dataMap = new HashMap<>(DATA_MAP);
		dataMap.put("jsonField", Map.of("lang", "Kotlin", "version", 1.8, "design", "Event Sourcing"));
		dataMap.put("jsonMultivaluedField", List.of(
				Map.of("lang", ".Net", "version", 4, "design", "Ambassador"),
				Map.of("lang", ".Net", "version", 4.8, "design", "Circuit Breaker")
		));

		var actual = dictDataService.create(buildDictDataItem(TESTABLE_JSON_DICT_ID, dataMap));

		assertEquals(3, ((Map<String, Object>) actual.getData().get("jsonField")).size());
		assertEquals(2, ((List<Map<String, Object>>) actual.getData().get("jsonMultivaluedField")).size());
	}

	@SneakyThrows
	protected void createDictItemWithGeoJsonObject()
	{
		var geo = objectMapper.readValue("{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"Point\",\"coordinates\":[37.412284,55.603515]}},{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[37.413423,55.604283],[37.41255,55.60361],[37.413995,55.602974],[37.414842,55.603629],[37.413423,55.604283]]]}},{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"LineString\",\"coordinates\":[[37.411999,55.603159],[37.413568,55.602384],[37.41522,55.603516]]}}]}",
				GeoJsonObject.class);
		var geoJson = objectMapper.writeValueAsString(geo);

		var dataMap = new HashMap<>(DATA_MAP);
		dataMap.put("geoJsonField", geoJson);
		dataMap.put("geoJsonMultivaluedField", List.of(geoJson, geoJson));
		dataMap.put("stringField", "geoJsonTest");

		dictDataService.create(buildDictDataItem(TESTABLE_GEO_JSON_DICT_ID, dataMap));

		var actual = dictDataService.getByFilter(TESTABLE_GEO_JSON_DICT_ID, List.of("*"), "stringField = 'geoJsonTest'", Pageable.unpaged())
				.getContent();

		var geoJsonFeature = actual.get(0).getData().get("geoJsonField");
		assertInstanceOf(FeatureCollection.class, geoJsonFeature);
		assertEquals(3, ((FeatureCollection) geoJsonFeature).getFeatures().size());

		var geoJsonFeatures = (List<Map<String, Object>>) actual.get(0).getData().get("geoJsonMultivaluedField");
		assertEquals(2, geoJsonFeatures.size());

		assertInstanceOf(FeatureCollection.class, geoJsonFeatures.get(0));
		assertEquals(3, ((FeatureCollection) geoJsonFeatures.get(0)).getFeatures().size());
		assertEquals(3, ((FeatureCollection) geoJsonFeatures.get(1)).getFeatures().size());
	}

	protected void createDictItemWithDefaultFields(String dictId)
	{
		var field1Value = "not default value";
		var field4Value = "test value";
		Map<String, Object> dataMap = Map.of(
				"field1", field1Value,
				"field4", field4Value);

		var actual = dictDataService.create(buildDictDataItem(dictId, dataMap));

		var actualData = actual.getData();

		assertEquals(field1Value, actualData.get("field1"));
		assertEquals(2, ((Number) actualData.get("field2")).intValue());
		assertEquals(2.0, ((Number) actualData.get("field3")).doubleValue());
		assertEquals(field4Value, actualData.get("field4"));
		assertEquals("defaultValue", actualData.get("field5"));
		assertEquals("defaultValue", actualData.get("field6"));
	}

	protected void createDictItemWithIncorrectStringFieldSize(String dictId)
	{
		Map<String, Object> dataMap = Map.of(
				"field1", "qwertyuiopasdfghjklzxcvbnm1234567890",
				"field4", "test value");

		Executable ex = () -> dictDataService.create(buildDictDataItem(dictId, dataMap));

		assertThrows(FieldValidationException.class, ex);
	}

	protected void createDictItemWithIncorrectIntegerFieldSize(String dictId)
	{
		Map<String, Object> dataMap = Map.of(
				"field2", -1,
				"field4", "test value");

		Executable ex = () -> dictDataService.create(buildDictDataItem(dictId, dataMap));

		assertThrows(FieldValidationException.class, ex);
	}

	protected void createDictItemWithIncorrectDecimalFieldSize(String dictId)
	{
		Map<String, Object> dataMap = Map.of(
				"field3", 30.1,
				"field4", "test value");

		Executable ex = () -> dictDataService.create(buildDictDataItem(dictId, dataMap));

		assertThrows(FieldValidationException.class, ex);
	}

	protected void updateDictItem()
	{
		var dictItem = dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, DATA_MAP));

		var timestampFields = new ArrayList<>((List<Object>) DATA_MAP.get("timestampField"));
		timestampFields.add("2021-08-15T11:00:00.000Z");

		var updatedDataMap = new HashMap<>(DATA_MAP);
		updatedDataMap.put("integerField", 3);
		updatedDataMap.put("timestampField", timestampFields);
		updatedDataMap.put("booleanField", false);

		var actual = dictDataService.update(dictItem.getId(), buildDictDataItem(TESTABLE_DICT_ID, updatedDataMap), dictItem.getVersion());

		assertEquals(dictItem.getVersion() + 1, actual.getVersion());

		assertEquals(3, ((List<Object>) actual.getData().get("timestampField")).size());
	}

	protected void updateDictItemWithEmptyMultivaluedData()
	{
		var dictItem = dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, DATA_MAP));

		var updatedDataMap = new HashMap<>(DATA_MAP);
		updatedDataMap.put("timestampField", null);

		var actual = dictDataService.update(dictItem.getId(), buildDictDataItem(TESTABLE_DICT_ID, updatedDataMap), dictItem.getVersion());

		assertEquals(0, ((List<Object>) actual.getData().get("timestampField")).size());

		dictItem = dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, DATA_MAP));

		updatedDataMap = new HashMap<>(DATA_MAP);
		updatedDataMap.put("timestampField", List.of());

		actual = dictDataService.update(dictItem.getId(), buildDictDataItem(TESTABLE_DICT_ID, updatedDataMap), dictItem.getVersion());

		assertEquals(0, ((List<Object>) actual.getData().get("timestampField")).size());
	}

	protected void updateDictItemWithJson()
	{
		var dataMap = new HashMap<>(DATA_MAP);

		dataMap = new HashMap<>(DATA_MAP);
		dataMap.put("jsonField", Map.of("lang", "Kotlin", "version", "1.8", "design", "Event Sourcing"));
		dataMap.put("jsonMultivaluedField", List.of(
				Map.of("lang", ".Net", "version", "4", "design", "Ambassador"),
				Map.of("lang", ".Net", "version", "4.8", "design", "Circuit Breaker")
		));

		var dictItem = dictDataService.create(buildDictDataItem(TESTABLE_JSON_DICT_ID, dataMap));

		var updatableMap = new HashMap<>(dictItem.getData());
		updatableMap.put("jsonField", Map.of("lang", "Kotlin", "version", "1.7", "design", "Event Sourcing"));
		updatableMap.put("jsonMultivaluedField", List.of(
				Map.of("lang", ".Net", "version", "4.1", "design", "Ambassador"),
				Map.of("lang", "Java", "version", "8", "design", "2PC")
		));

		dictDataService.update(dictItem.getId(), buildDictDataItem(TESTABLE_JSON_DICT_ID, updatableMap), dictItem.getVersion());

		var actual = dictDataService.getById(TESTABLE_JSON_DICT_ID, dictItem.getId());

		assertEquals(updatableMap, actual.getData());
	}

	@SneakyThrows
	protected void updateDictItemWithGeoJson()
	{
		var geo1 = objectMapper.readValue("{\"type\":\"Feature\",\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[55.68154659727316,37.553090921089115],[55.646161942996564,37.58087155165372]]]}}", GeoJsonObject.class);
		var geo1Json = objectMapper.writeValueAsString(geo1);

		var geo2 = objectMapper.readValue("{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"Point\",\"coordinates\":[37.412284,55.603515]}},{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[37.413423,55.604283],[37.41255,55.60361],[37.413995,55.602974],[37.414842,55.603629],[37.413423,55.604283]]]}},{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"LineString\",\"coordinates\":[[37.411999,55.603159],[37.413568,55.602384],[37.41522,55.603516]]}}]}", GeoJsonObject.class);
		var geo2Json = objectMapper.writeValueAsString(geo2);

		var dataMap = new HashMap<>(DATA_MAP);
		dataMap.put("geoJsonField", geo1Json);
		dataMap.put("geoJsonMultivaluedField", List.of(geo1Json, geo2Json));

		var dictItem = dictDataService.create(buildDictDataItem(TESTABLE_GEO_JSON_DICT_ID, dataMap));

		var updatableMap = new HashMap<>(dictItem.getData());

		var updatableGeo = objectMapper.readValue("{\"type\":\"Feature\",\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[55.68154659711111,37.553090921011111],[55.646161144006644,37.58087155668877]]]}}", GeoJsonObject.class);
		var updatableGeoJson = objectMapper.writeValueAsString(updatableGeo);

		updatableMap.put("geoJsonField", updatableGeoJson);
		updatableMap.put("geoJsonMultivaluedField", List.of(updatableGeoJson));

		dictDataService.update(dictItem.getId(), buildDictDataItem(TESTABLE_GEO_JSON_DICT_ID, updatableMap), dictItem.getVersion());

		var actual = dictDataService.getById(TESTABLE_GEO_JSON_DICT_ID, dictItem.getId());

		assertEquals(Feature.class, actual.getData().get("geoJsonField").getClass());
		assertEquals(updatableGeo, actual.getData().get("geoJsonField"));
		assertEquals(1, ((List<GeoJsonObject>) actual.getData().get("geoJsonMultivaluedField")).size());

		assertNotNull(actual.getHistory().get(0));

		var geoJsonHistoryFeature = (Map<String, Object>) actual.getHistory().get(0).get("geoJsonField");

		assertNotNull(geoJsonHistoryFeature);
		assertNotNull(geoJsonHistoryFeature.get("type"));
		assertEquals("Feature", geoJsonHistoryFeature.get("type"));

		geoJsonHistoryFeature = ((List<Map<String, Object>>) actual.getHistory().get(0).get("geoJsonMultivaluedField")).get(0);

		assertNotNull(geoJsonHistoryFeature);
		assertNotNull(geoJsonHistoryFeature.get("type"));
		assertEquals("Feature", geoJsonHistoryFeature.get("type"));
	}

	protected void updateConcurrentExc()
	{
		var dictItem = dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, DATA_MAP));

		var updatedDataMap = new HashMap<>(DATA_MAP);
		updatedDataMap.put("integerField", 3);

		dictDataService.update(dictItem.getId(), buildDictDataItem(TESTABLE_DICT_ID, updatedDataMap), dictItem.getVersion());

		assertThrows(DictConcurrentUpdateException.class,
				() -> dictDataService.update(dictItem.getId(), buildDictDataItem(TESTABLE_DICT_ID, updatedDataMap), dictItem.getVersion()));
	}

	protected void deleteDictItem()
	{
		var dictItem = dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, DATA_MAP));

		dictDataService.delete(TESTABLE_DICT_ID, dictItem.getId());

		assertThrows(ObjectNotFoundException.class, () -> dictDataService.getById(TESTABLE_DICT_ID, dictItem.getId()));
	}

	protected void deleteAllDictItems()
	{
		dictDataService.create(buildDictDataItem(TESTABLE_TRUNCATED_DICT_ID, DATA_MAP));

		dictDataService.deleteAll(TESTABLE_TRUNCATED_DICT_ID);

		assertEquals(0, dictDataService.countByFilter(TESTABLE_TRUNCATED_DICT_ID, null));
	}

	protected void concurrentCreateRefDictItemPossible()
	{
		var refDictItem = dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, DATA_MAP));
		var refItemId = refDictItem.getId();

		var refDataMap = new HashMap<>(DATA_MAP);
		refDataMap.put(TESTABLE_DICT_ID, refItemId);

		var latch = new CountDownLatch(1);

		var createThread = new Thread(() ->
				transactionTemplate.execute(status -> {
					try
					{
						dictDataService.create(buildDictDataItem(TESTABLE_REF_DICT_ID, refDataMap));

						latch.countDown();

						Thread.sleep(1000);
					}
					catch (Exception ignored)
					{
						//ignored
					}

					return null;
				}));

		var secondCreateThread = new Thread(() -> {
			try
			{
				latch.await();

				dictDataService.create(buildDictDataItem(TESTABLE_REF_DICT_ID, refDataMap));
			}
			catch (Exception ignored)
			{
				//ignored
			}
		});

		createThread.start();
		secondCreateThread.start();

		try
		{
			createThread.join();
			secondCreateThread.join();
		}
		catch (InterruptedException ignored)
		{
			//ignored
		}

		assertEquals(14, dictDataService.countByFilter(TESTABLE_REF_DICT_ID, null));
	}

	protected void createDictItemWithDeletedRefBlocked()
	{
		var refDictItem = dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, DATA_MAP));
		var refItemId = refDictItem.getId();

		var refDataMap = new HashMap<>(DATA_MAP);
		refDataMap.put(TESTABLE_DICT_ID, refItemId);

		var latch = new CountDownLatch(1);

		var deleteThread = new Thread(() ->
				transactionTemplate.execute(status -> {
					try
					{
						dictDataService.delete(TESTABLE_DICT_ID, refItemId);

						latch.countDown();

						Thread.sleep(1000);
					}
					catch (Exception ignored)
					{
						//ignored
					}

					return null;
				}));

		var createThreadException = new AtomicReference<Throwable>();

		var createThread = new Thread(() -> {
			try
			{
				latch.await();

				dictDataService.create(buildDictDataItem(TESTABLE_REF_DICT_ID, refDataMap));
			}
			catch (Exception exception)
			{
				createThreadException.set(exception.getCause());
			}
		});

		deleteThread.start();
		createThread.start();

		try
		{
			deleteThread.join();
			createThread.join();
		}
		catch (InterruptedException ignored)
		{
			//ignored
		}

		assertFalse(dictDataService.existsById(TESTABLE_DICT_ID, refItemId));
		assertInstanceOf(DictItemCreateException.class, createThreadException.get());
		assertEquals(27, dictDataService.countByFilter(TESTABLE_REF_DICT_ID, null));
	}

	protected void deleteRefDictItemBlocked()
	{
		var refDictItem = dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, DATA_MAP));
		var refItemId = refDictItem.getId();

		var refDataMap = new HashMap<>(DATA_MAP);
		refDataMap.put(TESTABLE_DICT_ID, refItemId);

		var latch = new CountDownLatch(1);

		var createThread = new Thread(() ->
				transactionTemplate.execute(status -> {
					try
					{
						dictDataService.create(buildDictDataItem(TESTABLE_REF_DICT_ID, refDataMap));

						latch.await();
					}
					catch (Exception ignored)
					{
						//ignored
					}

					return null;
				}));

		var deleteThread = new Thread(() -> {
			try
			{
				dictDataService.delete(TESTABLE_DICT_ID, refItemId);
			}
			catch (Exception ignored)
			{
				//ignored
			}

			latch.countDown();
		});

		createThread.start();
		deleteThread.start();

		try
		{
			createThread.join();
			deleteThread.join();
		}
		catch (InterruptedException ignored)
		{
			//ignored
		}

		assertTrue(dictDataService.existsById(TESTABLE_DICT_ID, refItemId));
	}

	private Attachment createAttachment(Resource fileResource) throws IOException
	{
		var bytes = IOUtils.toByteArray(fileResource.getInputStream());
		var attachment = attachmentService.addAttachment(Objects.requireNonNull(fileResource.getFilename()), MediaType.IMAGE_PNG_VALUE, UserInfo.SYSTEM_USER_ID, bytes);
		var savedData = attachmentService.getAttachmentData(attachment.getId());

		assertEquals(attachment.getSize(), bytes.length);
		assertArrayEquals(bytes, IOUtils.toByteArray(savedData.getInputStream()));

		return attachment;
	}

	private String createDictHierarchy()
	{
		final var stringField = "stringField";
		final var integerField = "integerField";
		final var doubleField = "doubleField";
		final var timestampField = "timestampField";
		final var booleanField = "booleanField";

		Supplier<Map<String, Object>> testDataMapFactory = () -> ImmutableMap.of(
				stringField, RandomStringUtils.randomAlphabetic(10),
				integerField, RandomUtils.nextInt(0, 128),
				doubleField, RandomUtils.nextDouble(0.0, 128.0),
				timestampField, DateConstants.ISO_OFFSET_DATE_TIME_MS_FORMATTER.format(ZonedDateTime.now()),
				booleanField, RandomUtils.nextBoolean());

		Function<String, Map<String, Object>> testRefDataMapFactory = (String id) ->
				ImmutableMap.<String, Object>builder()
						.putAll(testDataMapFactory.get())
						.put(TESTABLE_DICT_ID, id)
						.build();

		IntStream.range(0, 5)
				.boxed()
				.map(i -> testDataMapFactory.get())
				.map(dataMap -> dictDataService.create(buildDictDataItem(TESTABLE_DICT_ID, dataMap)).getId())
				.map(testRefDataMapFactory)
				.forEach(dataMap -> dictDataService.create(buildDictDataItem(TESTABLE_REF_DICT_ID, dataMap)));

		return integerField;
	}

	protected DictDataItem buildDictDataItem(String dictId, Map<String, Object> dataMap)
	{
		return DictDataItem.of(dictId, dataMap);
	}
}
