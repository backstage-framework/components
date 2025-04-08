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

import com.backstage.app.dict.common.CommonTest;
import com.backstage.app.dict.data.TestDictDataFactory;
import com.backstage.app.dict.data.TestDictFactory;
import com.backstage.app.dict.domain.DictFieldName;
import com.backstage.app.dict.exception.dict.DictException;
import com.backstage.app.dict.exception.dict.DictNotFoundException;
import com.backstage.app.dict.exception.dict.UnavailableDictRefException;
import com.backstage.app.dict.exception.dict.field.FieldNotFoundException;
import com.backstage.app.dict.service.validation.DictDataValidationService;
import org.apache.commons.lang3.RandomStringUtils;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CommonDictDataValidationServiceTest extends CommonTest
{
	@Autowired
	protected DictDataValidationService dictDataValidationService;

	@Autowired
	protected TestDictFactory testDictFactory;

	@Autowired
	protected TestDictDataFactory testDictDataFactory;

	protected String getDictId()
	{
		return "%s%s".formatted(dictsProperties.getStorage(), RandomStringUtils.random(3, true, false));
	}

	protected void validateSelectFields()
	{
		var dictId = testDictFactory.createNewDict(getDictId()).getId();
		var refDictId = testDictFactory.createReferenceDict(dictId).getId();

		dictDataValidationService.validateSelectFields(
				dictService.getById(refDictId),
				List.of(new DictFieldName(dictId, "integerField"),
						new DictFieldName(dictId, "created"),
						new DictFieldName(dictId, "stringField")));

		testDictFactory.eraseDictAndRefDict(dictId, refDictId);
	}

	protected void validateSelectFieldsFieldNotExisted()
	{
		var dictId = testDictFactory.createNewDict(getDictId()).getId();
		var refDictId = testDictFactory.createReferenceDict(dictId).getId();

		Executable result = () -> dictDataValidationService.validateSelectFields(
				dictService.getById(refDictId),
				List.of(new DictFieldName(dictId, "stringField"),
						new DictFieldName(dictId, "incorrect")));

		assertThrows(FieldNotFoundException.class, result);

		testDictFactory.eraseDictAndRefDict(dictId, refDictId);
	}

	protected void validateSelectFieldsIncorrect()
	{
		var dictId = testDictFactory.createNewDict(getDictId()).getId();

		Executable result = () -> dictDataValidationService.validateSelectFields(
				dictService.getById(dictId),
				List.of(new DictFieldName(dictId, "integerField")));

		assertThrows(UnavailableDictRefException.class, result);

		testDictFactory.eraseDict(dictId);
	}

	protected void validateSelectFieldsDictNotExisted()
	{
		Executable result = () -> dictDataValidationService.validateSelectFields(dictService.getById("incorrect"), List.of());

		assertThrows(DictNotFoundException.class, result);
	}

	protected void validateDictData()
	{
		Map<String, Object> stringDateMap = Map.of(
				"stringField", "string",
				"integerField", 1,
				"doubleField", BigDecimal.valueOf(Double.parseDouble("2.354")),
				"timestampField", "2021-08-15T06:00:00.000Z",
				"booleanField", true);

		Map<String, Object> objectDateMap = Map.of(
				"stringField", "string",
				"integerField", 1,
				"doubleField", BigDecimal.valueOf(Double.parseDouble("2.55532")),
				"timestampField", new Date(),
				"booleanField", Boolean.TRUE);

		//dictDataValidationService.validateDictDataItem(buildDictDataItem(TESTABLE_DICT_ID, stringDateMap), USER_ID);
		//dictDataValidationService.validateDictDataItem(buildDictDataItem(TESTABLE_DICT_ID, objectDateMap), USER_ID);
	}

	protected void validateDictDataDictNotExisted()
	{
		Map<String, Object> map = Map.of(
				"stringField", "string",
				"integerField", 1,
				"timestampField", "2021-08-15T06:00:00.000Z");

		//assertThrows(DictNotFoundException.class, () -> dictDataValidationService.validateDictDataItem(buildDictDataItem("incorect", map), USER_ID));
	}

	protected void validateDictDataNoRequiredField()
	{
		Map<String, Object> map = Map.of("stringField", "string");

		//var e = assertThrows(FieldValidationException.class, () -> dictDataValidationService.validateDictDataItem(buildDictDataItem(TESTABLE_DICT_ID, map), USER_ID));
		//assertEquals(e.getMessage(), "Отсутствует обязательное поле: integerField.");
	}

	protected void validateDictDataExistsField()
	{
		Map<String, Object> duplicatedFiled = Map.of(
				"stringField11", "text",
				"timestampField", "2021-08-15T06:00:00.000Z");

		//assertThrows(ForbiddenFieldNameException.class, () -> dictDataValidationService.validateDictDataItem(buildDictDataItem(TESTABLE_DICT_ID, duplicatedFiled), USER_ID));
	}

	protected void validateDictDataUnexpectedMultivalued()
	{
		Map<String, Object> map = Map.of(
				"stringField", "string",
				"integerField", List.of(1, 2),
				"timestampField", "2021-08-15T06:00:00.000Z");

		//var e = assertThrows(FieldValidationException.class, () -> dictDataValidationService.validateDictDataItem(buildDictDataItem(TESTABLE_DICT_ID, map), USER_ID));
		//assertEquals(e.getMessage(), "Не может быть массивом: integerField.");
	}

	protected void validateDictDataFieldCastException()
	{
		Map<String, Object> map = new HashMap<>(Map.of(
				"stringField", "string",
				"integerField", 1,
				"doubleField", BigDecimal.valueOf(Double.parseDouble("2.123")),
				"booleanField", false,
				"timestampField", "incorrect"));

		//assertThrows(FieldValidationException.class, () -> dictDataValidationService.validateDictDataItem(buildDictDataItem(TESTABLE_DICT_ID, map), USER_ID));

		map.remove("timestampField");
		map.put("doubleField", Decimal128.parse("22.33"));

		//assertThrows(FieldValidationException.class, () -> dictDataValidationService.validateDictDataItem(buildDictDataItem(TESTABLE_DICT_ID, map), USER_ID));

		map.put("doubleField", BigDecimal.valueOf(Double.parseDouble("2.123")));
		map.put("booleanField", "true");

		//assertThrows(FieldValidationException.class, () -> dictDataValidationService.validateDictDataItem(buildDictDataItem(TESTABLE_DICT_ID, map), USER_ID));
	}

	protected void validateDictDataForbiddenField()
	{
		Map<String, Object> map = Map.of(
				"stringField", "string",
				"created", "incorrect",
				"integerField", 1,
				"timestampField", "2021-08-15T06:00:00.000Z");

		//assertThrows(ForbiddenFieldNameException.class, () -> dictDataValidationService.validateDictDataItem(buildDictDataItem(TESTABLE_DICT_ID, map), USER_ID));
	}

	protected void deleteRefDictItemForbidden()
	{
		var dictId = testDictFactory.createNewDict(getDictId()).getId();
		var refDictId = testDictFactory.createReferenceDict(dictId).getId();
		var dictItemId = testDictDataFactory.createDefaultItem(dictId).getId();
		testDictDataFactory.createDefaultItemWithCustomField(refDictId, Map.of(dictId, dictItemId));

		Executable result = () -> dictDataValidationService.validateDelete(dictService.getById(dictId), dictItemId);

		assertThrows(DictException.class, result);
	}

	protected void deleteAllRefDictItemsForbidden()
	{
		var dictId = testDictFactory.createNewDict(getDictId()).getId();
		var refDictId = testDictFactory.createReferenceDict(dictId).getId();
		var dictItemId = testDictDataFactory.createDefaultItem(dictId).getId();
		testDictDataFactory.createDefaultItemWithCustomField(refDictId, Map.of(dictId, dictItemId));

		Executable result = () -> dictDataValidationService.validateDeleteAll(dictService.getById(dictId));

		assertThrows(DictException.class, result);
	}
}
