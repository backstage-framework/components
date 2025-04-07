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
import com.backstage.app.dict.data.TestDictDataFactory;
import com.backstage.app.dict.data.TestDictFactory;
import com.backstage.app.dict.domain.DictField;
import com.backstage.app.dict.domain.DictItem;
import com.backstage.app.dict.exception.dict.DictConcurrentUpdateException;
import com.backstage.app.dict.exception.dict.field.FieldNotFoundException;
import com.backstage.app.dict.exception.dict.field.FieldValidationException;
import com.backstage.app.dict.model.dictitem.DictDataItem;
import com.backstage.app.dict.service.advice.AttachmentDictDataServiceAdvice;
import com.backstage.app.exception.ObjectNotFoundException;
import com.backstage.app.model.other.user.UserInfo;
import com.backstage.app.utils.StreamCollectors;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Comparators;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.ThrowingConsumer;
import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.GeoJsonObject;
import org.junit.jupiter.api.*;
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
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

//todo декомпозиция тестовых кейсов (?)
//todo общий рефакторинг с переходом на константы и методы из фабрик
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CommonDictDataServiceTest extends CommonTest
{
	//todo убрать после рефакторингн тестов конверсий
	protected static String TESTABLE_DICT_ID;
	protected static String TESTABLE_REF_DICT_ID;
	protected static String TESTABLE_GEO_JSON_DICT_ID;

	@Autowired
	private AttachmentService attachmentService;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	protected TestDictFactory testDictFactory;

	@Autowired
	protected TestDictDataFactory testDictDataFactory;

	//todo возможно стоит весь блок attachment вынесити в отдельные тесты\фабрики
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

	//todo убрать отсюда
	protected static final Map<String, Object> DATA_MAP = Map.of(
			"stringField", "string",
			"integerField", 1,
			"doubleField", 2.558,
			"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T08:00:00.000Z"),
			"stringFieldMultivalued", List.of("one", "two", "three"),
			"booleanField", true);

	protected String getDictId()
	{
		return "%s%s".formatted(dictsProperties.getStorage(), RandomStringUtils.random(3, true, false));
	}

	//todo весь блок attachment вынесити в отдельные тесты\фабрики
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

	//todo вернуться к методу, когда будут отрефатчены все тесты, пока удаляем руками
//	@BeforeEach
//	public void eraseDicts()
//	{
//		testDictFactory.eraseDicts();
//	}

	private final ThrowingConsumer<Object> objectWriter = obj -> System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj));

	protected void getByIds()
	{
		var dictId = testDictFactory.createNewDict(getDictId())
				.getId();
		var ids = testDictDataFactory.createManyWithDefaultValues(dictId, 10)
				.stream()
				.map(DictItem::getId)
				.toList();

		assertArrayEquals(ids.toArray(String[]::new), dictDataService.getByIds(dictId, ids).stream().map(DictItem::getId).toArray(String[]::new));
	}

	protected void getDistinctValuesByFilterWithoutFilter()
	{
		var dictId = testDictFactory.createNewDict(getDictId())
				.getId();
		testDictDataFactory.createManyWithRandomStringFieldValues(dictId, TestDictFactory.STRING_FIELD, 3);

		var result = dictDataService.getDistinctValuesByFilter(dictId, TestDictFactory.STRING_FIELD, "");

		assertEquals(3, result.size());

		testDictFactory.eraseDict(dictId);
	}

	protected void getDistinctValuesByFilter()
	{
		var dictId = testDictFactory.createNewDict(getDictId())
				.getId();
		testDictDataFactory.createManyWithRandomStringFieldValues(dictId, TestDictFactory.STRING_FIELD, 6);

		var result = dictDataService.getDistinctValuesByFilter(dictId, TestDictFactory.STRING_FIELD,  TestDictFactory.STRING_FIELD + " != null");

		assertEquals(6, result.size());

		testDictFactory.eraseDict(dictId);
	}

	@Test
	protected void streamByFilter()
	{
		var dictId = testDictFactory.createNewDict(getDictId())
				.getId();
		testDictDataFactory.createManyWithRandomStringFieldValues(dictId, TestDictFactory.STRING_FIELD, 10);

		var query = "stringField like 'string'";

		try (var stream = dictDataService.streamByFilter(dictId, List.of("*"), query))
		{
			var itemCount = dictDataService.getByFilter(dictId, List.of("*"), query, PageRequest.of(0, 10)).getTotalElements();

			assertEquals(itemCount, stream.count());
		}

		testDictFactory.eraseDict(dictId);
	}

	protected void getByFilterWithNullRefField()
	{
		var dictId = testDictFactory.createNewDict(getDictId())
				.getId();
		var refDictId = testDictFactory.createReferenceDict(getDictId())
				.getId();
		var createdItemId = testDictDataFactory.createDefaultItemWithCustomField(refDictId, Map.of("stringField", "nullableDictRefField"))
				.getId();

		var result = dictDataService.getByFilter(refDictId, List.of("*", dictId + ".*"), "stringField = 'nullableDictRefField'", PageRequest.of(0, 10));

		assertEquals(1, result.getContent().size());
		assertEquals(result.getContent().get(0).getId(), createdItemId);

		testDictFactory.eraseDictAndRefDict(dictId, refDictId);
	}

	protected void getByFilter()
	{
		var dictId = testDictFactory.createNewDict(getDictId())
				.getId();
		testDictDataFactory.createManyWithDefaultValues(dictId, 10);

		var result = dictDataService.getByFilter(dictId, List.of("*"),
				"stringField like 'string' and (integerField = 1 or integerField in (2, 5, 8) and integerField != 10 or integerField <= 2 and doubleField > 1.9 and doubleField < 2.1)",
				PageRequest.of(0, 10));

		var allMatchStringField = result.getContent()
				.stream()
				.map(DictItem::getData)
				.map(Map::entrySet)
				.flatMap(Collection::stream)
				.filter(it -> "stringField".equals(it.getKey()))
				.map(Map.Entry::getValue)
				.allMatch(TestDictDataFactory.STRING_FIELD_VALUE::equals);

		assertFalse(result.getContent().isEmpty());
		assertTrue(allMatchStringField);

		testDictFactory.eraseDict(dictId);
	}

	protected void getByFilterWithPrefixLikeExpression()
	{
		var dictId = testDictFactory.createNewDict(getDictId())
				.getId();
		testDictDataFactory.createManyWithDefaultValues(dictId, 10);

		var queryValue = "str";

		var result = dictDataService.getByFilter(dictId, List.of("*"),
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

		testDictFactory.eraseDict(dictId);
	}

	protected void getByFilterWithInnerLikeExpression()
	{
		var dictId = testDictFactory.createNewDict(getDictId())
				.getId();
		testDictDataFactory.createManyWithDefaultValues(dictId, 10);

		var queryValue = "rin";

		var result = dictDataService.getByFilter(dictId, List.of("*"),
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

		testDictFactory.eraseDict(dictId);
	}

	protected void getByFilterWithPostfixLikeExpression()
	{
		var dictId = testDictFactory.createNewDict(getDictId())
				.getId();
		testDictDataFactory.createManyWithDefaultValues(dictId, 10);

		var queryValue = "ring";

		var result = dictDataService.getByFilter(dictId, List.of("*"),
				"stringField like '%%%s' and stringField ilike '%%%s'".formatted(queryValue, queryValue.toUpperCase()),
				PageRequest.of(0, 10));

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

		testDictFactory.eraseDict(dictId);
	}

	protected void getByFilterWithUnderscoreLikeExpression()
	{
		var dictId = testDictFactory.createNewDict(getDictId())
				.getId();
		testDictDataFactory.createManyWithDefaultValues(dictId, 10);

		var result = dictDataService.getByFilter(dictId, List.of("*"),
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

		testDictFactory.eraseDict(dictId);
	}

	protected void getByFilterWithEscapeLikeSpecialSymbols()
	{
		var dictId = testDictFactory.createNewDict(getDictId())
				.getId();
		var dictItemId = testDictDataFactory.createDefaultItemWithCustomField(dictId, Map.of("stringField", "st%i_g"))
				.getId();

		var result = dictDataService.getByFilter(dictId, List.of("*"),
				"stringField like 'st\\%i\\_g' and stringField ilike 'ST\\%I\\_G'",
				PageRequest.of(0, 10));

		assertEquals(1, result.getContent().size());
		assertEquals(result.getContent().get(0).getId(), dictItemId);

		testDictFactory.eraseDict(dictId);
	}

	protected void getByFilterWithLogicalExpression()
	{
		var dictId = testDictFactory.createNewDict(getDictId())
				.getId();
		var dictItemId = testDictDataFactory.createDefaultItemWithCustomField(dictId, Map.of("stringField", "stringFieldLogicalExpressionTest"))
				.getId();

		var result = dictDataService.getByFilter(dictId, List.of("*"),
				"stringField = 'stringFieldLogicalExpressionTest' and (doubleField = 2.558 or integerField = 1)",
				PageRequest.of(0, 10));

		assertEquals(1, result.getContent().size());
		assertEquals(result.getContent().get(0).getId(), dictItemId);

		testDictFactory.eraseDict(dictId);
	}

	protected void getIdsByFilter()
	{
		var dictId = testDictFactory.createNewDict(getDictId())
				.getId();
		testDictDataFactory.createManyWithDefaultValues(dictId, 1);

		var result = dictDataService.getIdsByFilter(dictId, "integerField = 1 or stringField like 'string' or integerField in (2, 5, 8) and integerField != 10 or integerField <= 2 and doubleField > 1.9 and doubleField < 2.1");

		assertNotNull(result.getContent().get(0));
	}

	//TODO: тест - с ambiguous состоянием указанных поле в сортировке
	protected void getByFilterInnerDictSort()
	{
		var dictId = testDictFactory.createNewDict(getDictId())
				.getId();
		var refDictId = testDictFactory.createReferenceDict(getDictId())
				.getId();
		testDictDataFactory.createDictHierarchy(dictId, refDictId, 10);

		var result = dictDataService.getByFilter(refDictId, List.of("*", dictId + ".*"), "%s != null".formatted(dictId),
						PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, dictId + "." + TestDictDataFactory.INTEGER_FIELD)))
				.getContent()
				.stream()
				.map(DictItem::getData)
				.toList();

		objectWriter.accept(result);

		boolean actual = Comparators.isInOrder(result, Comparator.comparing((Map<String, Object> data) -> (Long) ((DictItem) data.get(dictId)).getData().get(TestDictDataFactory.INTEGER_FIELD)).reversed());

		assertTrue(actual);

		testDictFactory.eraseDictAndRefDict(dictId, refDictId);
	}

	protected void getByFilterInnerDictSortWrongFiledName()
	{
		var dictId = testDictFactory.createNewDict(getDictId())
				.getId();
		var refDictId = testDictFactory.createReferenceDict(getDictId())
				.getId();
		testDictDataFactory.createDictHierarchy(dictId, refDictId, 10);

		assertThrows(FieldNotFoundException.class, () -> dictDataService.getByFilter(refDictId, List.of("*", dictId + ".*"), null,
				PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, dictId + "." + "wrongFiled"))));

		testDictFactory.eraseDictAndRefDict(dictId, refDictId);
	}

	protected void getByFilterDictSortServiceField()
	{
		var dictId = testDictFactory.createNewDict(getDictId())
				.getId();
		testDictDataFactory.createManyWithDefaultValues(dictId, 10);

		var sortedIdFields = dictDataService.getByFilter(dictId, List.of("*"), null,
						PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, ServiceFieldConstants.ID)))
				.getContent()
				.stream()
				.map(DictItem::getId)
				.toList();

		var sortedCreatedFields = dictDataService.getByFilter(dictId, List.of("*"), null,
						PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, ServiceFieldConstants.CREATED)))
				.getContent()
				.stream()
				.map(DictItem::getCreated)
				.toList();

		var actualIdFields = Comparators.isInOrder(sortedIdFields, Comparator.reverseOrder());
		var actualCreatedFields = Comparators.isInOrder(sortedCreatedFields, Comparator.reverseOrder());

		assertTrue(actualIdFields);
		assertTrue(actualCreatedFields);

		testDictFactory.eraseDict(dictId);
	}

	//todo убрать передачу ожидаемого значения кол-ва элементов после фикса пагинации для MongoDB
	protected void getByFilterWithUnpagedSort(int totalElements)
	{
		var dictId = testDictFactory.createNewDict(getDictId())
				.getId();
		testDictDataFactory.createManyWithDefaultValues(dictId, totalElements);

		var result = dictDataService.getByFilter(dictId, List.of("*"), null,
						Pageable.unpaged(Sort.by(Sort.Direction.DESC, TestDictDataFactory.INTEGER_FIELD)))
				.getContent()
				.stream()
				.map(DictItem::getData)
				.map(map -> (Long) map.get(TestDictDataFactory.INTEGER_FIELD))
				.toList();

		assertEquals(totalElements, result.size());

		var actual = Comparators.isInOrder(result, Comparator.reverseOrder());

		assertTrue(actual);

		testDictFactory.eraseDict(dictId);
	}

	protected void getByFilterUnpaged()
	{
		var dictId = testDictFactory.createNewDict(getDictId())
				.getId();
		testDictDataFactory.createManyWithDefaultValues(dictId, 10);

		var result = dictDataService.getByFilter(dictId, List.of("*"), null, Pageable.unpaged());

		assertTrue(result.getPageable().isUnpaged());
		assertEquals(1, result.getTotalPages());

		testDictFactory.eraseDict(dictId);
	}

	protected void getByFilterDictSortDataField()
	{
		var dictId = testDictFactory.createNewDict(getDictId())
				.getId();
		testDictDataFactory.createManyWithRandomValues(dictId, 10);

		var result = dictDataService.getByFilter(dictId, List.of("*"), null,
						PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, TestDictDataFactory.INTEGER_FIELD)))
				.getContent()
				.stream()
				.map(DictItem::getData)
				.map(map -> (Long) map.get(TestDictDataFactory.INTEGER_FIELD))
				.toList();

		var actual = Comparators.isInOrder(result, Comparator.reverseOrder());

		assertTrue(actual);

		testDictFactory.eraseDict(dictId);
	}

	protected void getByFilterWithMultipleSortDataField()
	{
		var dictId = testDictFactory.createNewDict(getDictId())
				.getId();
		testDictDataFactory.createManyWithRandomValues(dictId, 10);

		var result = dictDataService.getByFilter(dictId, List.of("*"), "integerField != null and doubleField != null and id != 'with_id'", PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, TestDictDataFactory.INTEGER_FIELD, TestDictDataFactory.DOUBLE_FIELD)))
				.getContent();

		var resultSortedCorrectly = Comparators.isInOrder(result, Comparator.comparing(dictItem -> (Long) ((DictItem) dictItem).getData().get(TestDictDataFactory.INTEGER_FIELD))
				.thenComparing(dictItem -> (BigDecimal) ((DictItem) dictItem).getData().get(TestDictDataFactory.DOUBLE_FIELD)));

		assertTrue(resultSortedCorrectly);

		testDictFactory.eraseDict(dictId);
	}

	protected void getByFilterWithServiceSelectField()
	{
		var dictId = testDictFactory.createNewDict(getDictId())
				.getId();
		testDictDataFactory.createManyWithRandomValues(dictId, 20);

		var result = dictDataService.getByFilter(dictId, List.of(ServiceFieldConstants.CREATED),
						null, PageRequest.of(0, 20))
				.getContent()
				.stream()
				.map(it -> StringUtils.hasText(it.getId()) && it.getCreated() != null && it.getVersion() != null)
				.toList();

		result.forEach(Assertions::assertTrue);

		testDictFactory.eraseDict(dictId);
	}

	protected void getByFilterWithDifferentDateCorrect()
	{
		var dictId = testDictFactory.createNewDict(getDictId())
				.getId();

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

		var stringDateItemId = testDictDataFactory.createDefaultItemWithCustomField(dictId, stringLocalDateTimeMap).getId();
		var objectDateItemId = testDictDataFactory.createDefaultItemWithCustomField(dictId, objectLocalDateTimeMap).getId();
		var stringLocalDateTimeItemId = testDictDataFactory.createDefaultItemWithCustomField(dictId, stringDateDataMap).getId();
		var objectLocalDateTimeItemId = testDictDataFactory.createDefaultItemWithCustomField(dictId, objectDateDataMap).getId();

		var stringDateItem = dictDataService.getById(dictId, stringDateItemId);
		var objectDateItem = dictDataService.getById(dictId, objectDateItemId);
		var stringLocalDateTimeItem = dictDataService.getById(dictId, stringLocalDateTimeItemId);
		var objectLocalDateTimeItem = dictDataService.getById(dictId, objectLocalDateTimeItemId);

		assertEquals(stringDateItem.getData().get("timestampField"), List.of(localDateTime));
		assertEquals(objectDateItem.getData().get("timestampField"), List.of(localDateTime));
		assertEquals(stringLocalDateTimeItem.getData().get("timestampField"), List.of(localDateTime));
		assertEquals(objectLocalDateTimeItem.getData().get("timestampField"), List.of(localDateTime));

		testDictFactory.eraseDict(dictId);
	}

	//TODO: тест с выбором всех полей у refDict
	protected void getByFilterWithDictReference()
	{
		var dictId = testDictFactory.createNewDict(getDictId())
				.getId();
		var dictItemId = testDictDataFactory.createDefaultItem(dictId)
				.getId();
		var refDictId = testDictFactory.createReferenceDict(getDictId())
				.getId();
		testDictDataFactory.createDefaultItemWithCustomField(refDictId, Map.of(dictId, dictItemId));

		var result = dictDataService.getByFilter(refDictId, List.of("*", dictId + ".timestampField"), null, PageRequest.of(0, 10));

		assertNotNull(result.getContent().get(0).getData().get(dictId));
		assertEquals(DictItem.class, result.getContent().get(0).getData().get(dictId).getClass());
		assertTrue(((DictItem) result.getContent().get(0).getData().get(dictId)).getData().containsKey("timestampField"));

		testDictFactory.eraseDictAndRefDict(dictId, refDictId);
	}

	protected void getByFilterWithDictReferenceAllFieldSelect()
	{
		var dictId = testDictFactory.createNewDict(getDictId())
				.getId();
		var dictItemId = testDictDataFactory.createDefaultItem(dictId)
				.getId();
		var refDictId = testDictFactory.createReferenceDict(getDictId())
				.getId();
		var refItemId = testDictDataFactory.createDefaultItemWithCustomField(refDictId, Map.of(dictId, dictItemId))
				.getId();

		var result = dictDataService.getByFilter(refDictId, List.of("*"), "%s = '%s'".formatted(dictId, dictItemId), PageRequest.of(0, 10))
				.getContent();

		assertEquals(1, result.size());
		assertEquals(result.get(0).getId(), refItemId);
		assertEquals(7, result.get(0).getData().size());
		assertEquals(result.get(0).getData().get(dictId), dictItemId);

		testDictFactory.eraseDictAndRefDict(dictId, refDictId);
	}

	//TODO: тест с фильтрацией reference Dict по элементам массива
	protected void getByFilterWithQueryReference()
	{
		var dictId = testDictFactory.createNewDict(getDictId())
				.getId();
		Map<String, Object> refDictDataMap = Map.of("stringField", "queryReferenceDict");
		var dictItemId = testDictDataFactory.createDefaultItemWithCustomField(dictId, refDictDataMap)
				.getId();
		var refDictId = testDictFactory.createReferenceDict(getDictId())
				.getId();
		testDictDataFactory.createDefaultItemWithCustomField(refDictId, Map.of(dictId, dictItemId));

		var query = "integerField = 1 and %s.stringField = 'queryReferenceDict'".formatted(dictId);

		var actual = dictDataService.getByFilter(refDictId, List.of("*", dictId + ".stringField"), query, Pageable.unpaged())
				.getContent()
				.stream()
				.map(DictItem::getData)
				.map(Map::entrySet)
				.flatMap(Collection::stream)
				.filter(it -> dictId.equals(it.getKey()))
				.map(Map.Entry::getValue)
				.map(it -> (DictItem) it)
				.findFirst()
				.orElseThrow(() -> new RuntimeException("Query reference test failed."));

		assertEquals(dictItemId, actual.getId());
		assertEquals(refDictDataMap.get("stringField"), actual.getData().get("stringField"));

		testDictFactory.eraseDictAndRefDict(dictId, refDictId);
	}

	protected void getByFilterWithArrayContainsAnyValue()
	{
		var dictId = testDictFactory.createNewDict(getDictId())
				.getId();
		Map<String, Object> dataMap = Map.of("timestampField", List.of("2021-08-15T06:00:00.000Z", "2023-04-15T06:00:00.000Z", "2019-08-15T06:00:00.000Z"),
				"stringFieldMultivalued", List.of("one", "two", "three"));
		var dictItem = testDictDataFactory.createDefaultItemWithCustomField(dictId, dataMap);

		var anyQuery = "timestampField any ['2021-08-15T06:00:00'::timestamp, '2001-08-16T08:00:00'::timestamp, '2006-08-16T08:00:00'::timestamp] and stringFieldMultivalued any ['one']";

		var actualIds = dictDataService.getByFilter(dictId, List.of("*"), anyQuery, Pageable.unpaged())
				.getContent()
				.stream()
				.map(DictItem::getId)
				.collect(Collectors.toSet());

		assertTrue(actualIds.contains(dictItem.getId()));

		testDictFactory.eraseDict(dictId);
	}

	protected void getByFilterWithArrayContainsAllValue()
	{
		var dictId = testDictFactory.createNewDict(getDictId())
				.getId();
		Map<String, Object> dataMap = Map.of("timestampField", List.of("2021-08-15T06:00:00.000Z", "2001-08-16T08:00:00.000Z", "2006-08-16T08:00:00.000Z"),
				"stringFieldMultivalued", List.of("one", "two", "three"));
		var dictItem = testDictDataFactory.createDefaultItemWithCustomField(dictId, dataMap);

		var allQuery = "timestampField all ['2021-08-15T06:00:00'::timestamp, '2001-08-16T08:00:00'::timestamp, '2006-08-16T08:00:00'::timestamp] and stringFieldMultivalued all ['one', 'two', 'three']";

		var actualIds = dictDataService.getByFilter(dictId, List.of("*"), allQuery, Pageable.unpaged())
				.getContent()
				.stream()
				.map(DictItem::getId)
				.collect(Collectors.toSet());

		assertTrue(actualIds.contains(dictItem.getId()));

		testDictFactory.eraseDict(dictId);
	}

	protected void existsById()
	{
		var dictId = testDictFactory.createNewDict(getDictId())
				.getId();
		testDictDataFactory.createDefaultItem(dictId);

		var item = dictDataService.getByFilter(dictId, List.of("*"), "integerField = 1", PageRequest.of(0, 1))
				.toList()
				.get(0);

		assertTrue(dictDataService.existsById(dictId, item.getId()));
		assertFalse(dictDataService.existsById(dictId, "-1"));

		testDictFactory.eraseDict(dictId);
	}

	protected void existsByFilter()
	{
		var dictId = testDictFactory.createNewDict(getDictId())
				.getId();
		testDictDataFactory.createDefaultItem(dictId);

		assertTrue(dictDataService.existsByFilter(dictId, "integerField = 1"));

		assertFalse(dictDataService.existsByFilter(dictId, "integerField = 13"));

		testDictFactory.eraseDict(dictId);
	}

	protected void attachmentBindingWithCreateDictItem()
	{
		var dictId = testDictFactory.createAttachmentDict(getDictId())
				.getId();
		var attachmentDataMap = Map.of(
				"stringField", "string",
				"integerField", 1L,
				"doubleField", 2,
				"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T08:00:00.000Z"),
				"attachmentField", firstAttachmentId,
				"attachmentsField", List.of(firstAttachmentId),
				"booleanField", true);
		var dictItem = testDictDataFactory.createItemWithCustomFields(dictId, attachmentDataMap);

		var singleFieldAttachments = attachmentService.getAttachments(AttachmentDictDataServiceAdvice.DICT_ITEM_ATTACHMENT_TYPE, AttachmentDictDataServiceAdvice.getAttachmentOwnerId(dictId, dictItem));
		var multivaluedFieldAttachments = attachmentService.getAttachments(AttachmentDictDataServiceAdvice.DICT_ITEM_ATTACHMENT_TYPE, AttachmentDictDataServiceAdvice.getAttachmentOwnerId(dictId, dictItem));

		assertNotNull(dictItem);
		assertEquals(firstAttachment.getId(), singleFieldAttachments.get(0).getId());
		assertEquals(firstAttachment.getId(), multivaluedFieldAttachments.get(0).getId());

		testDictFactory.eraseDict(dictId);
	}

	protected void checkAttachmentBindingWithUpdateDictItem()
	{
		var dictId = testDictFactory.createAttachmentDict(getDictId())
				.getId();
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
		var dictItem = testDictDataFactory.createItemWithCustomFields(dictId, attachmentDataMap);

		var updatedDictItem = dictDataService.update(dictItem.getId(), buildDictDataItem(dictId, attachmentDataUpdateMap), dictItem.getVersion());

		var fieldAttachments = attachmentService.getAttachments(AttachmentDictDataServiceAdvice.DICT_ITEM_ATTACHMENT_TYPE, AttachmentDictDataServiceAdvice.getAttachmentOwnerId(dictId, updatedDictItem));
		var fieldAttachmentsIds = fieldAttachments.stream().map(Attachment::getId).toList();

		assertNotNull(dictItem);
		assertTrue(fieldAttachmentsIds.contains(secondAttachmentId));
		assertTrue(fieldAttachmentsIds.contains(thirdAttachmentId));
		assertEquals(2, fieldAttachmentsIds.size());

		testDictFactory.eraseDict(dictId);
	}

	protected void checkAttachmentReleaseWithDeleteDictItem()
	{
		var dictId = testDictFactory.createAttachmentDict(getDictId())
				.getId();
		var attachmentDataMap = Map.of(
				"stringField", "string",
				"integerField", 1L,
				"doubleField", 2,
				"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T08:00:00.000Z"),
				"booleanField", true,
				"attachmentField", firstAttachmentId,
				"attachmentsField", List.of(firstAttachmentId));
		var dictItem = testDictDataFactory.createItemWithCustomFields(dictId, attachmentDataMap);

		var dictItemAttachments = attachmentService.getAttachments(AttachmentDictDataServiceAdvice.DICT_ITEM_ATTACHMENT_TYPE, AttachmentDictDataServiceAdvice.getAttachmentOwnerId(dictId, dictItem));

		assertFalse(dictItemAttachments.isEmpty());
		assertEquals(firstAttachmentId, dictItemAttachments.get(0).getId());

		dictDataService.delete(dictId, dictItem.getId());

		dictItemAttachments = attachmentService.getAttachments(AttachmentDictDataServiceAdvice.DICT_ITEM_ATTACHMENT_TYPE, AttachmentDictDataServiceAdvice.getAttachmentOwnerId(dictId, dictItem));

		assertTrue(dictItemAttachments.isEmpty());

		testDictFactory.eraseDict(dictId);
	}

	protected void createDictItem()
	{
		var dictId = testDictFactory.createNewDict(getDictId())
				.getId();
		var item = testDictDataFactory.buildDefaultDictDataItem(dictId);

		assertNotNull(dictDataService.create(item));

		testDictFactory.eraseDict(dictId);
	}

	protected void createManyDictItems()
	{
		var dictId = testDictFactory.createNewDict(getDictId())
				.getId();
		var items = testDictDataFactory.buildManyDefaultDictDataItems(dictId, 3);

		assertNotNull(dictDataService.createMany(dictId, items));

		testDictFactory.eraseDict(dictId);
	}

	protected void createDictItemWithNullData()
	{
		var dictId = testDictFactory.createNewDict(getDictId())
				.getId();
		var dataMap = new HashMap<>(DATA_MAP);
		dataMap.put("doubleField", null);
		var item = testDictDataFactory.buildDictDataItem(dictId, dataMap);

		dictDataService.create(item, dictId);

		var actual = dictDataService.getByFilter(dictId, List.of("*"), "doubleField = null", Pageable.unpaged())
				.getContent()
				.stream()
				.map(DictItem::getData)
				.map(Map::entrySet)
				.flatMap(Collection::stream)
				.filter(it -> TestDictDataFactory.DOUBLE_FIELD.equals(it.getKey()))
				.collect(StreamCollectors.toLinkedHashMap(Map.Entry::getKey, Map.Entry::getValue));

		actual.values().forEach(Assertions::assertNull);

		testDictFactory.eraseDict(dictId);
	}

	protected void createWithDifferentType()
	{
		var dictId = testDictFactory.createNewDict(getDictId())
				.getId();
		var defaultItem = testDictDataFactory.buildDefaultDictDataItem(dictId);

		var longDataMap = Map.of(
				"stringField", "string",
				"integerField", 1L,
				"doubleField", 2,
				"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T08:00:00.000Z"),
				"booleanField", true);
		var longDataItem = testDictDataFactory.buildDictDataItem(dictId, longDataMap);

		var doubleDataMap = Map.of(
				"stringField", "string",
				"integerField", 1.0,
				"doubleField", 2.50,
				"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T08:00:00.000Z"),
				"booleanField", Boolean.TRUE);
		var doubleDataItem = testDictDataFactory.buildDictDataItem(dictId, doubleDataMap);

		assertNotNull(dictDataService.create(defaultItem));
		assertNotNull(dictDataService.create(longDataItem));
		assertNotNull(dictDataService.create(doubleDataItem));

		testDictFactory.eraseDict(dictId);
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

		var dictId = dictService.create(dict)
				.getId();

		var dictDataItem = buildDictDataItem(dictId, Map.of(fieldId, "1__numericStringValue"));
		var dictItem = dictDataService.create(dictDataItem);

		assertEquals(dictItem.getData().get(fieldId), dictDataItem.getDataItemMap().get(fieldId));

		var updatedDictDataItem = buildDictDataItem(dictId, Map.of(fieldId, "stringValue"));
		var updatedDictItem = dictDataService.update(dictItem.getId(), updatedDictDataItem, dictItem.getVersion());

		assertEquals(updatedDictItem.getData().get(fieldId), updatedDictDataItem.getDataItemMap().get(fieldId));
	}

	protected void createCorrectContainsFieldsInHistoryMap()
	{
		var dictId = testDictFactory.createNewDict(getDictId())
				.getId();

		var itemId = "item_id";

		var dataMap = new HashMap<>(DATA_MAP);
		dataMap.put(ServiceFieldConstants.ID, itemId);

		var dictItem = dictDataService.create(buildDictDataItem(dictId, dataMap));

		assertNotNull(dictItem.getHistory());
		assertTrue(dictItem.getHistory().isEmpty());

		var targetFieldName = TestDictDataFactory.DOUBLE_FIELD;

		dataMap = new HashMap<>(DATA_MAP);
		dataMap.put(targetFieldName, null);

		dictItem = dictDataService.update(itemId, buildDictDataItem(dictId, dataMap), dictItem.getVersion());

		assertNull(dictItem.getData().get(targetFieldName));
		assertNotNull(dictItem.getHistory());
		assertFalse(dictItem.getHistory().isEmpty());

		assertEquals(DATA_MAP.get(targetFieldName), dictItem.getHistory().get(0).get(targetFieldName));

		testDictFactory.eraseDict(dictId);
	}

	protected void createCorrectContainsFieldsInHistoryMapForSkippedNullValue()
	{
		var dictId = testDictFactory.createNewDict(getDictId())
				.getId();

		var itemId = "skipped_null_item_id";

		var dataMap = new HashMap<>(DATA_MAP);
		dataMap.put(ServiceFieldConstants.ID, itemId);

		var dictItem = dictDataService.create(buildDictDataItem(dictId, dataMap));

		assertNotNull(dictItem.getHistory());
		assertTrue(dictItem.getHistory().isEmpty());

		var targetFieldName = "doubleField";

		dataMap = new HashMap<>(DATA_MAP);
		dataMap.remove(targetFieldName);

		dictItem = dictDataService.update(itemId, buildDictDataItem(dictId, dataMap), dictItem.getVersion());

		assertNull(dictItem.getData().get(targetFieldName));
		assertNotNull(dictItem.getHistory());
		assertFalse(dictItem.getHistory().isEmpty());

		assertEquals(DATA_MAP.get(targetFieldName), dictItem.getHistory().get(0).get(targetFieldName));

		testDictFactory.eraseDict(dictId);
	}

	//TODO: расширить тест когда Json указывается строкой
	protected void createDictItemWithJson()
	{
		var dictId = testDictFactory.createJsonDict(getDictId())
				.getId();

		var dataMap = new HashMap<>(DATA_MAP);

		dataMap = new HashMap<>(DATA_MAP);
		dataMap.put("jsonField", Map.of("lang", "Kotlin", "version", 1.8, "design", "Event Sourcing"));
		dataMap.put("jsonMultivaluedField", List.of(
				Map.of("lang", ".Net", "version", 4, "design", "Ambassador"),
				Map.of("lang", ".Net", "version", 4.8, "design", "Circuit Breaker")
		));

		var actual = dictDataService.create(buildDictDataItem(dictId, dataMap));

		assertEquals(3, ((Map<String, Object>) actual.getData().get("jsonField")).size());
		assertEquals(2, ((List<Map<String, Object>>) actual.getData().get("jsonMultivaluedField")).size());

		testDictFactory.eraseDict(dictId);
	}

	@SneakyThrows
	protected void createDictItemWithGeoJsonObject()
	{
		var dictId = testDictFactory.createGeoJsonDict(getDictId())
				.getId();

		var geo = objectMapper.readValue("{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"Point\",\"coordinates\":[37.412284,55.603515]}},{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[37.413423,55.604283],[37.41255,55.60361],[37.413995,55.602974],[37.414842,55.603629],[37.413423,55.604283]]]}},{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"LineString\",\"coordinates\":[[37.411999,55.603159],[37.413568,55.602384],[37.41522,55.603516]]}}]}",
				GeoJsonObject.class);
		var geoJson = objectMapper.writeValueAsString(geo);

		var dataMap = new HashMap<>(DATA_MAP);
		dataMap.put("geoJsonField", geoJson);
		dataMap.put("geoJsonMultivaluedField", List.of(geoJson, geoJson));
		dataMap.put("stringField", "geoJsonTest");

		dictDataService.create(buildDictDataItem(dictId, dataMap));

		var actual = dictDataService.getByFilter(dictId, List.of("*"), "stringField = 'geoJsonTest'", Pageable.unpaged())
				.getContent();

		var geoJsonFeature = actual.get(0).getData().get("geoJsonField");
		assertInstanceOf(FeatureCollection.class, geoJsonFeature);
		assertEquals(3, ((FeatureCollection) geoJsonFeature).getFeatures().size());

		var geoJsonFeatures = (List<Map<String, Object>>) actual.get(0).getData().get("geoJsonMultivaluedField");
		assertEquals(2, geoJsonFeatures.size());

		assertInstanceOf(FeatureCollection.class, geoJsonFeatures.get(0));
		assertEquals(3, ((FeatureCollection) geoJsonFeatures.get(0)).getFeatures().size());
		assertEquals(3, ((FeatureCollection) geoJsonFeatures.get(1)).getFeatures().size());

		testDictFactory.eraseDict(dictId);
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

		//todo: отвязать тесты от testDict2 т.к. он создается в миграции
//		testDictFactory.eraseDict(dictId);
	}

	protected void createDictItemWithIncorrectStringFieldSize(String dictId)
	{
		Map<String, Object> dataMap = Map.of(
				"field1", "qwertyuiopasdfghjklzxcvbnm1234567890",
				"field4", "test value");

		Executable ex = () -> dictDataService.create(buildDictDataItem(dictId, dataMap));

		assertThrows(FieldValidationException.class, ex);

		//todo: отвязать тесты от testDict2 т.к. он создается в миграции
//		testDictFactory.eraseDict(dictId);
	}

	protected void createDictItemWithIncorrectIntegerFieldSize(String dictId)
	{
		Map<String, Object> dataMap = Map.of(
				"field2", -1,
				"field4", "test value");

		Executable ex = () -> dictDataService.create(buildDictDataItem(dictId, dataMap));

		assertThrows(FieldValidationException.class, ex);

		//todo: отвязать тесты от testDict2 т.к. он создается в миграции
//		testDictFactory.eraseDict(dictId);
	}

	protected void createDictItemWithIncorrectDecimalFieldSize(String dictId)
	{
		Map<String, Object> dataMap = Map.of(
				"field3", 30.1,
				"field4", "test value");

		Executable ex = () -> dictDataService.create(buildDictDataItem(dictId, dataMap));

		assertThrows(FieldValidationException.class, ex);

		//todo: отвязать тесты от testDict2 т.к. он создается в миграции
//		testDictFactory.eraseDict(dictId);
	}

	protected void updateDictItem()
	{
		var dictId = testDictFactory.createNewDict(getDictId()).getId();
		var dictItem = testDictDataFactory.createDefaultItem(dictId);

		var timestampFields = new ArrayList<>((List<Object>) DATA_MAP.get("timestampField"));
		timestampFields.add("2021-08-15T11:00:00.000Z");

		var updatedDataMap = new HashMap<>(DATA_MAP);
		updatedDataMap.put("integerField", 3);
		updatedDataMap.put("timestampField", timestampFields);
		updatedDataMap.put("booleanField", false);

		var actual = dictDataService.update(dictItem.getId(), buildDictDataItem(dictId, updatedDataMap), dictItem.getVersion());

		assertEquals(dictItem.getVersion() + 1, actual.getVersion());

		assertEquals(3, ((List<Object>) actual.getData().get("timestampField")).size());

		testDictFactory.eraseDict(dictId);
	}

	protected void updateDictItemWithEmptyMultivaluedData()
	{
		var dictId = testDictFactory.createNewDict(getDictId()).getId();
		var dictItem = testDictDataFactory.createDefaultItem(dictId);

		var updatedDataMap = new HashMap<>(DATA_MAP);
		updatedDataMap.put("timestampField", null);

		var actual = dictDataService.update(dictItem.getId(), buildDictDataItem(dictId, updatedDataMap), dictItem.getVersion());

		assertEquals(0, ((List<Object>) actual.getData().get("timestampField")).size());

		dictItem = dictDataService.create(buildDictDataItem(dictId, DATA_MAP));

		updatedDataMap = new HashMap<>(DATA_MAP);
		updatedDataMap.put("timestampField", List.of());

		actual = dictDataService.update(dictItem.getId(), buildDictDataItem(dictId, updatedDataMap), dictItem.getVersion());

		assertEquals(0, ((List<Object>) actual.getData().get("timestampField")).size());

		testDictFactory.eraseDict(dictId);
	}

	protected void updateDictItemWithJson()
	{
		var dictId = testDictFactory.createJsonDict(getDictId()).getId();

		var dataMap = new HashMap<>(DATA_MAP);

		dataMap = new HashMap<>(DATA_MAP);
		dataMap.put("jsonField", Map.of("lang", "Kotlin", "version", "1.8", "design", "Event Sourcing"));
		dataMap.put("jsonMultivaluedField", List.of(
				Map.of("lang", ".Net", "version", "4", "design", "Ambassador"),
				Map.of("lang", ".Net", "version", "4.8", "design", "Circuit Breaker")
		));

		var dictItem = testDictDataFactory.createItemWithCustomFields(dictId, dataMap);

		var updatableMap = new HashMap<>(dictItem.getData());
		updatableMap.put("jsonField", Map.of("lang", "Kotlin", "version", "1.7", "design", "Event Sourcing"));
		updatableMap.put("jsonMultivaluedField", List.of(
				Map.of("lang", ".Net", "version", "4.1", "design", "Ambassador"),
				Map.of("lang", "Java", "version", "8", "design", "2PC")
		));

		dictDataService.update(dictItem.getId(), buildDictDataItem(dictId, updatableMap), dictItem.getVersion());

		var actual = dictDataService.getById(dictId, dictItem.getId());

		assertEquals(updatableMap, actual.getData());

		testDictFactory.eraseDict(dictId);
	}

	@SneakyThrows
	protected void updateDictItemWithGeoJson()
	{
		var dictId = testDictFactory.createGeoJsonDict(getDictId()).getId();

		var geo1 = objectMapper.readValue("{\"type\":\"Feature\",\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[55.68154659727316,37.553090921089115],[55.646161942996564,37.58087155165372]]]}}", GeoJsonObject.class);
		var geo1Json = objectMapper.writeValueAsString(geo1);

		var geo2 = objectMapper.readValue("{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"Point\",\"coordinates\":[37.412284,55.603515]}},{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[37.413423,55.604283],[37.41255,55.60361],[37.413995,55.602974],[37.414842,55.603629],[37.413423,55.604283]]]}},{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"LineString\",\"coordinates\":[[37.411999,55.603159],[37.413568,55.602384],[37.41522,55.603516]]}}]}", GeoJsonObject.class);
		var geo2Json = objectMapper.writeValueAsString(geo2);

		var dataMap = new HashMap<>(DATA_MAP);
		dataMap.put("geoJsonField", geo1Json);
		dataMap.put("geoJsonMultivaluedField", List.of(geo1Json, geo2Json));

		var dictItem = dictDataService.create(buildDictDataItem(dictId, dataMap));

		var updatableMap = new HashMap<>(dictItem.getData());

		var updatableGeo = objectMapper.readValue("{\"type\":\"Feature\",\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[55.68154659711111,37.553090921011111],[55.646161144006644,37.58087155668877]]]}}", GeoJsonObject.class);
		var updatableGeoJson = objectMapper.writeValueAsString(updatableGeo);

		updatableMap.put("geoJsonField", updatableGeoJson);
		updatableMap.put("geoJsonMultivaluedField", List.of(updatableGeoJson));

		dictDataService.update(dictItem.getId(), testDictDataFactory.buildDictDataItem(dictId, updatableMap), dictItem.getVersion());

		var actual = dictDataService.getById(dictId, dictItem.getId());

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

		testDictFactory.eraseDict(dictId);
	}

	protected void updateConcurrentExc()
	{
		var dictId = testDictFactory.createNewDict(getDictId())
				.getId();
		var updatedDataMap = new HashMap<>(DATA_MAP);
		updatedDataMap.put("integerField", 3);
		var dictItem = testDictDataFactory.createItemWithCustomFields(dictId, updatedDataMap);

		dictDataService.update(dictItem.getId(), buildDictDataItem(dictId, updatedDataMap), dictItem.getVersion());

		assertThrows(DictConcurrentUpdateException.class,
				() -> dictDataService.update(dictItem.getId(), buildDictDataItem(dictId, updatedDataMap), dictItem.getVersion()));

		testDictFactory.eraseDict(dictId);
	}

	protected void deleteDictItem()
	{
		var dictId = testDictFactory.createNewDict(getDictId())
				.getId();
		var dictItem = testDictDataFactory.createDefaultItem(dictId);

		dictDataService.delete(dictId, dictItem.getId());

		assertThrows(ObjectNotFoundException.class, () -> dictDataService.getById(dictId, dictItem.getId()));

		testDictFactory.eraseDict(dictId);
	}

	protected void deleteAllDictItems()
	{
		var dictId = testDictFactory.createNewDict(getDictId())
				.getId();
		testDictDataFactory.createManyWithRandomValues(dictId, 3);

		dictDataService.deleteAll(dictId);

		assertEquals(0, dictDataService.countByFilter(dictId, null));

		testDictFactory.eraseDict(dictId);
	}

	protected void deleteRefDictItemBlocked()
	{
		var dictId = testDictFactory.createNewDict(getDictId())
				.getId();
		var refDictId = testDictFactory.createReferenceDict(getDictId())
				.getId();

		var item = testDictDataFactory.createDefaultItem(dictId);

		var refDataMap = new HashMap<>(DATA_MAP);
		refDataMap.put(dictId, item.getId());

		var latch = new CountDownLatch(1);

		var createThread = new Thread(() ->
				transactionTemplate.execute(status -> {
					try
					{
						dictDataService.create(buildDictDataItem(refDictId, refDataMap));

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
				dictDataService.delete(dictId, item.getId());
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

		assertTrue(dictDataService.existsById(dictId, item.getId()));

		testDictFactory.eraseDictAndRefDict(dictId, refDictId);
	}

	//todo вынести создание аттачей
	private Attachment createAttachment(Resource fileResource) throws IOException
	{
		var bytes = IOUtils.toByteArray(fileResource.getInputStream());
		var attachment = attachmentService.addAttachment(Objects.requireNonNull(fileResource.getFilename()), MediaType.IMAGE_PNG_VALUE, UserInfo.SYSTEM_USER_ID, bytes);
		var savedData = attachmentService.getAttachmentData(attachment.getId());

		assertEquals(attachment.getSize(), bytes.length);
		assertArrayEquals(bytes, IOUtils.toByteArray(savedData.getInputStream()));

		return attachment;
	}

	//todo перейти на метод фабрики
	protected DictDataItem buildDictDataItem(String dictId, Map<String, Object> dataMap)
	{
		return DictDataItem.of(dictId, dataMap);
	}
}
