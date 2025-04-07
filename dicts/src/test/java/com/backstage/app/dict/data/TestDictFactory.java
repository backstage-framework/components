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

import com.backstage.app.dict.api.domain.DictFieldType;
import com.backstage.app.dict.configuration.properties.DictsProperties;
import com.backstage.app.dict.domain.*;
import com.backstage.app.dict.service.DictService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.backstage.app.dict.constant.ServiceFieldConstants.ID;

//todo подумать над объединением логики dictFactory и dataFactory, возможно стоит вынести общие константы и сервисы в родителя.
@Component
@RequiredArgsConstructor
public class TestDictFactory
{
	public static final String STRING_FIELD = "stringField";
	public static final String INTEGER_FIELD = "integerField";
	public static final String STRING_DEFAULT_VALUE = "defaultValue";

	private final DictService dictService;

	public Dict createNewDict(String dictId)
	{
		return createNewDict(dictId, new DictEngine(DictsProperties.DEFAULT_ENGINE));
	}

	public Dict createReferenceDict(String dictId)
	{
		var refDict = buildDict(dictId + "dataRef", new DictEngine(DictsProperties.DEFAULT_ENGINE));

		addReferenceField(refDict, dictId);

		return dictService.create(refDict);
	}

	public Dict createAttachmentDict(String dictId)
	{
		var attachDict = buildDict(dictId + "dataAttach", new DictEngine(DictsProperties.DEFAULT_ENGINE));

		addAttachmentField(attachDict);

		return dictService.create(attachDict);
	}

	public Dict createJsonDict(String dictId)
	{
		var jsonDict = buildDict(dictId + "dataJson", new DictEngine(DictsProperties.DEFAULT_ENGINE));

		addJsonField(jsonDict);

		return dictService.create(jsonDict);
	}

	public Dict createGeoJsonDict(String dictId)
	{
		var jsonDict = buildDict(dictId + "dataGeoJson", new DictEngine(DictsProperties.DEFAULT_ENGINE));

		addGeoJsonField(jsonDict);

		return dictService.create(jsonDict);
	}

	//todo вернуться к методу, когда будут отрефатчены все тесты, пока удаляем руками
//	public void eraseDicts()
//	{
//		dictService.getAll()
//				.stream()
//				.sorted(Comparator.comparing(
//						dict -> dict.getFields().stream()
//								.noneMatch(field -> field.getDictRef() != null),
//						Comparator.naturalOrder()
//				))
//				.map(Dict::getId)
//				.forEach(dictService::delete);
//	}

	public void eraseDict(String dictId)
	{
		dictService.delete(dictId);
	}

	public void eraseDictAndRefDict(String dictId, String refDictId)
	{
		dictService.delete(refDictId);
		dictService.delete(dictId);
	}

	private Dict createNewDict(String dictId, DictEngine dictEngine)
	{
		return dictService.create(buildDict(dictId, dictEngine));
	}

	private Dict buildDict(String dictId, DictEngine dictEngine)
	{
		return Dict.builder()
				.id(withRandom(dictId))
				.fields(buildFields())
				.indexes(new ArrayList<>(List.of(buildIndex(dictId, STRING_FIELD), buildIndex(dictId, INTEGER_FIELD))))
				.constraints(new ArrayList<>(List.of(buildConstraint(dictId, INTEGER_FIELD))))
				.enums(new ArrayList<>(List.of(buildEnum(dictId), buildEnum(dictId))))
				.engine(dictEngine)
				.build();
	}

	private Dict buildEmptyDict(String dictId, DictEngine dictEngine)
	{
		return Dict.builder()
				.id(withRandom(dictId))
				.engine(dictEngine)
				.build();
	}

	private String withRandom(String dictId)
	{
		return "%s%s".formatted(dictId, RandomStringUtils.random(3, true, false));
	}

	private DictIndex buildIndex(String dictId, String... fieldIds)
	{
		return DictIndex.builder()
				.id(withRandom(dictId))
				.direction(Sort.Direction.DESC)
				.fields(Arrays.asList(fieldIds))
				.build();
	}

	private DictConstraint buildConstraint(String dictId, String... fieldIds)
	{
		return DictConstraint.builder()
				.id(withRandom(dictId))
				.fields(Arrays.asList(fieldIds))
				.build();
	}

	private List<DictField> buildFields()
	{
		var fields = new ArrayList<DictField>();

		fields.add(
				DictField.builder()
						.id("stringField")
						.name("строка")
						.type(DictFieldType.STRING)
						.required(true)
						.multivalued(false)
						.defaultValue(STRING_DEFAULT_VALUE)
						.build()
		);

		fields.add(
				DictField.builder()
						.id("stringFieldMultivalued")
						.name("строка (множественное)")
						.type(DictFieldType.STRING)
						.required(false)
						.multivalued(true)
						.build()
		);

		fields.add(
				DictField.builder()
						.id("integerField")
						.name("число")
						.type(DictFieldType.INTEGER)
						.required(false)
						.multivalued(false)
						.build()
		);

		fields.add(
				DictField.builder()
						.id("doubleField")
						.name("вещественное число")
						.type(DictFieldType.DECIMAL)
						.required(false)
						.multivalued(false)
						.build()
		);

		fields.add(
				DictField.builder()
						.id("timestampField")
						.name("Дата и время")
						.type(DictFieldType.TIMESTAMP)
						.required(false)
						.multivalued(true)
						.build()
		);

		fields.add(
				DictField.builder()
						.id("booleanField")
						.name("Булево")
						.type(DictFieldType.BOOLEAN)
						.required(false)
						.multivalued(false)
						.build()
		);

		return fields;
	}

	private void addReferenceField(Dict refDict, String dictId)
	{
		refDict.getFields()
				.add(DictField.builder()
						.id(dictId)
						.name("Ссылка")
						.type(DictFieldType.DICT)
						.required(false)
						.multivalued(false)
						.dictRef(new DictFieldName(dictId, ID))
						.build());

	}

	private void addAttachmentField(Dict attachDict)
	{
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
	}

	private void addJsonField(Dict jsonDict)
	{
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
	}

	private void addGeoJsonField(Dict geoJsonDict)
	{
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
	}

	private DictEnum buildEnum(String dictId)
	{
		return DictEnum.builder()
				.id(withRandom(dictId))
				.name("enum_name_" + dictId)
				.values(Set.of("value_1_" + dictId, "value_2_" + dictId))
				.build();
	}
}

