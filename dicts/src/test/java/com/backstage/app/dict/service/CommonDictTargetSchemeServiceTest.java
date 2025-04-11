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

package com.backstage.app.dict.service;

import com.backstage.app.dict.api.domain.DictFieldType;
import com.backstage.app.dict.common.CommonTest;
import com.backstage.app.dict.constant.ServiceFieldConstants;
import com.backstage.app.dict.domain.*;
import com.backstage.app.dict.domain.scheme.DictNativeScheme;
import com.backstage.app.dict.domain.scheme.FieldNativeScheme;
import com.backstage.app.dict.service.backend.postgres.PostgresEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.backstage.app.dict.service.backend.postgres.PostgresDictSchemeBackend.COMPLEX_FIELD_TYPES;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CommonDictTargetSchemeServiceTest extends CommonTest
{
	protected void getNativeScheme(String dictId, String engine)
	{
		var dict = createDict(dictId, new DictEngine(engine));

		var expected = buildExpectedScheme(dict);

		var actual = dictService.getNativeScheme(dict.getId());

		assertEquals(expected, actual);
	}

	private DictNativeScheme buildExpectedScheme(Dict dict)
	{
		var engine = dict.getEngine();
		var engineName = engine.getName();

		var dictId = dict.getId();

		var fields = dict.getFields()
				.stream()
				.map(it -> FieldNativeScheme.builder()
						.fieldId(it.getId())
						.columnId(it.getId())
						.fullColumnId("%s.%s".formatted(dictId, it.getId()))
						.nativeType(getNativeType(it, engineName))
						.build())
				.toList();

		return DictNativeScheme.builder()
				.dictId(dictId)
				.tableId(engineName.equals(PostgresEngine.POSTGRES) ? "dicts.%s".formatted(dictId) : dictId)
				.fields(fields)
				.engine(engine)
				.build();
	}

	private Dict createDict(String dictId, DictEngine dictEngine)
	{
		var refDict = createRefDict(dictEngine.getName());

		var dict = dictService.create(buildTestDict(dictId, dictEngine, refDict.getId())).copy();

		var testEnum = buildTestEnum("enumId");
		dict.getEnums().add(testEnum);

		var fields = dict.getFields()
				.stream()
				.filter(it -> !ServiceFieldConstants.getServiceSchemeFields().contains(it.getId()))
				.collect(Collectors.toList());
		fields.addAll(buildTestEnumFields(testEnum.getId()));
		dict.setFields(fields);

		dict = dictService.update(dict.getId(), dict);

		return dict;
	}

	private Dict createRefDict(String dictEngine)
	{
		var refDict = new Dict();
		refDict.setId(withRandom("refDict" + dictEngine));
		refDict.setName("refDict");

		return dictService.create(refDict);
	}

	private Dict buildTestDict(String dictId, DictEngine dictEngine, String refDictId)
	{
		var expectedDict = new Dict();

		expectedDict.setId(withRandom(dictId + dictEngine.getName()));
		expectedDict.setFields(buildTestFields(refDictId));
		expectedDict.setEngine(dictEngine);

		return expectedDict;
	}

	private List<DictField> buildTestFields(String refDictId)
	{
		var fields = new ArrayList<DictField>();

		fields.add(
				DictField.builder()
						.id("integerField")
						.name("число")
						.type(DictFieldType.INTEGER)
						.multivalued(false)
						.build()
		);

		fields.add(
				DictField.builder()
						.id("integerFieldMultivalued")
						.name("число (множественное)")
						.type(DictFieldType.INTEGER)
						.multivalued(true)
						.build()
		);

		fields.add(
				DictField.builder()
						.id("doubleField")
						.name("вещественное число")
						.type(DictFieldType.DECIMAL)
						.multivalued(false)
						.build()
		);

		fields.add(
				DictField.builder()
						.id("doubleFieldMultivalued")
						.name("вещественное число (множественное)")
						.type(DictFieldType.DECIMAL)
						.multivalued(true)
						.build()
		);

		fields.add(
				DictField.builder()
						.id("stringField")
						.name("строка")
						.type(DictFieldType.STRING)
						.multivalued(false)
						.build()
		);

		fields.add(
				DictField.builder()
						.id("stringFieldMultivalued")
						.name("строка (множественное)")
						.type(DictFieldType.STRING)
						.multivalued(true)
						.build()
		);

		fields.add(
				DictField.builder()
						.id("booleanField")
						.name("Булево")
						.type(DictFieldType.BOOLEAN)
						.multivalued(false)
						.build()
		);

		fields.add(
				DictField.builder()
						.id("booleanFieldMultivalued")
						.name("Булево (множественное)")
						.type(DictFieldType.BOOLEAN)
						.multivalued(true)
						.build()
		);

		fields.add(
				DictField.builder()
						.id("dictField")
						.name("Булево")
						.type(DictFieldType.DICT)
						.dictRef(new DictFieldName(refDictId, "id"))
						.multivalued(false)
						.build()
		);

		fields.add(
				DictField.builder()
						.id("dictMultivalued")
						.name("Булево (множественное)")
						.type(DictFieldType.DICT)
						.dictRef(new DictFieldName(refDictId, "id"))
						.multivalued(true)
						.build()
		);

		fields.add(
				DictField.builder()
						.id("dateField")
						.name("Дата")
						.type(DictFieldType.DATE)
						.multivalued(false)
						.build()
		);

		fields.add(
				DictField.builder()
						.id("dateFieldMultivalued")
						.name("Дата (множественное)")
						.type(DictFieldType.DATE)
						.multivalued(true)
						.build()
		);

		fields.add(
				DictField.builder()
						.id("timestampField")
						.name("Дата и время")
						.type(DictFieldType.TIMESTAMP)
						.multivalued(false)
						.build()
		);

		fields.add(
				DictField.builder()
						.id("timestampFieldMultivalued")
						.name("Дата и время (множественное)")
						.type(DictFieldType.TIMESTAMP)
						.multivalued(true)
						.build()
		);

		fields.add(
				DictField.builder()
						.id("attachmentField")
						.name("Вложение")
						.type(DictFieldType.ATTACHMENT)
						.multivalued(false)
						.build()
		);

		fields.add(
				DictField.builder()
						.id("attachmentMultivalued")
						.name("Вложение (множественное)")
						.type(DictFieldType.ATTACHMENT)
						.multivalued(true)
						.build()
		);

		fields.add(
				DictField.builder()
						.id("geoJsonField")
						.name("geoJson")
						.type(DictFieldType.GEO_JSON)
						.multivalued(false)
						.build()
		);

		fields.add(
				DictField.builder()
						.id("geoJsonMultivalued")
						.name("geoJson (множественное)")
						.type(DictFieldType.GEO_JSON)
						.multivalued(true)
						.build()
		);

		return fields;
	}

	private DictEnum buildTestEnum(String enumId)
	{
		return DictEnum.builder()
				.id(enumId)
				.name(enumId)
				.values(Set.of("value_1_" + enumId, "value_2_" + enumId))
				.build();
	}

	private List<DictField> buildTestEnumFields(String enumId)
	{
		return List.of(
				DictField.builder()
						.id("enumField")
						.name("Енам")
						.type(DictFieldType.ENUM)
						.multivalued(false)
						.enumId(enumId)
						.build(),
				DictField.builder()
						.id("enumFieldMultivalued")
						.name("Енам (множественное)")
						.type(DictFieldType.ENUM)
						.multivalued(true)
						.enumId(enumId)
						.build()
		);
	}

	private String getNativeType(DictField it, String engineName)
	{
		if (!engineName.equals(PostgresEngine.POSTGRES))
		{
			return null;
		}

		return computeNativePostgresType(it);
	}

	private String computeNativePostgresType(DictField field)
	{
		var singleType = switch (field.getType())
		{
			case INTEGER -> "bigint";
			case DECIMAL -> "numeric";
			case STRING, DICT, ENUM, ATTACHMENT -> "text"; //TODO: рассмотреть varchar с max ограничением символов на уровне движка БД
			case BOOLEAN -> "boolean";
			case DATE -> "date";
			case TIMESTAMP -> "timestamp";
			case JSON -> "jsonb default '%s'::jsonb".formatted(field.isMultivalued() ? "[]" : "{}");
			case GEO_JSON -> "jsonb%1$s default %2$s".formatted(field.isMultivalued() ? "[]" : "", field.isMultivalued() ? "array[]::jsonb[]" : "'{}'");
		};

		if (field.isMultivalued() && !COMPLEX_FIELD_TYPES.contains(field.getType()))
		{
			singleType += "[]";
		}

		if (field.isRequired())
		{
			singleType += " not null";
		}

		return singleType;
	}
}
