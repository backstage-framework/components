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

package com.backstage.app.dict.common;

import com.backstage.app.database.configuration.db.MongoConfiguration;
import com.backstage.app.dict.api.domain.DictFieldType;
import com.backstage.app.dict.configuration.backend.provider.DictSchemeBackendProvider;
import com.backstage.app.dict.configuration.properties.DictsProperties;
import com.backstage.app.dict.domain.*;
import com.backstage.app.dict.model.dictitem.DictDataItem;
import com.backstage.app.dict.service.DictDataService;
import com.backstage.app.dict.service.DictService;
import com.backstage.app.dict.service.backend.DictBackend;
import com.backstage.app.dict.service.backend.DictTransactionBackend;
import com.backstage.app.dict.service.backend.VersionSchemeBackend;
import com.backstage.app.dict.service.migration.ClasspathMigrationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;
import java.util.*;

import static com.backstage.app.dict.constant.ServiceFieldConstants.getServiceSchemeFields;

@SpringBootTest
@ContextConfiguration(initializers = {AppDataSourceInitializer.class, MongoInitializer.class})
@Import({JacksonAutoConfiguration.class, MongoConfiguration.class})
public class CommonTest
{
	public static final String MONGO_DICT_ID = "mgDict";
	public static final String POSTGRES_DICT_ID = "pgDict";

	public static final String STRING_DEFAULT_VALUE = "defaultValue";

	@Autowired
	protected ObjectMapper objectMapper;

	@Autowired
	protected DictsProperties dictsProperties;

	@Autowired
	protected DictService dictService;

	@Autowired
	protected DictDataService dictDataService;

	@Autowired
	protected ClasspathMigrationService classpathMigrationService;

	@Autowired
	protected DictSchemeBackendProvider schemeBackendProvider;

	@Autowired
	protected DictBackend dictBackend;

	@Autowired
	protected VersionSchemeBackend versionSchemeBackend;

	@Autowired
	protected DictTransactionBackend dictTransactionBackend;

	protected Dict createNewDict(String dictId)
	{
		return createNewDict(dictId, new DictEngine(DictsProperties.DEFAULT_ENGINE));
	}

	protected Dict createNewDict(String dictId, DictEngine dictEngine)
	{
		return dictService.create(buildDict(dictId, dictEngine));
	}

	protected Dict buildDict(String dictId)
	{
		return buildDict(dictId, new DictEngine(DictsProperties.DEFAULT_ENGINE));
	}

	protected Dict buildDict(String dictId, DictEngine dictEngine)
	{
		var expectedDict = new Dict();

		expectedDict.setId(withRandom(dictId));
		expectedDict.setFields(buildFields());
		expectedDict.setIndexes(new ArrayList<>(List.of(buildIndex(dictId, "stringField"), buildIndex(dictId, "integerField"))));
		expectedDict.setConstraints(new ArrayList<>(List.of(buildConstraint(dictId, "integerField"))));
		expectedDict.setEnums(new ArrayList<>(List.of(buildEnum(dictId), buildEnum(dictId))));
		expectedDict.setEngine(dictEngine);

		return expectedDict;
	}

	protected List<DictField> buildFields()
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
						.id("integerField")
						.name("число")
						.type(DictFieldType.INTEGER)
						.required(true)
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
						.required(true)
						.multivalued(false)
						.build()
		);

		return fields;
	}

	protected DictIndex buildIndex(String dictId, String... fieldIds)
	{
		return DictIndex.builder()
				.id(withRandom(dictId))
				.direction(Sort.Direction.DESC)
				.fields(Arrays.asList(fieldIds))
				.build();
	}

	protected DictEnum buildEnum(String dictId)
	{
		return DictEnum.builder()
				.id(withRandom(dictId))
				.name("enum_name_" + dictId)
				.values(Set.of("value_1_" + dictId, "value_2_" + dictId))
				.build();
	}

	protected DictConstraint buildConstraint(String dictId, String... fieldIds)
	{
		return DictConstraint.builder()
				.id(withRandom(dictId))
				.fields(Arrays.asList(fieldIds))
				.build();
	}

	protected void addDictData(String dictId)
	{
		var dataMap = new HashMap<String, Object>(
				Map.of(
						"stringField", "string",
						"integerField", 1L,
						"doubleField", BigDecimal.valueOf(Double.parseDouble("2.776")),
						"timestampField", "2021-08-15T06:00:00.000Z",
						"booleanField", true)
		);

		dictDataService.create(DictDataItem.of(dictId, dataMap));
	}

	protected String withRandom(String dictId)
	{
		return "%s%s".formatted(dictId, RandomStringUtils.random(3, true, false));
	}

	protected List<DictField> withoutServiceFields(List<DictField> source)
	{
		return source.stream()
				.filter(it -> !getServiceSchemeFields().contains(it.getId()))
				.toList();
	}

	protected String generateRandomUUIDWithoutDashes()
	{
		return UUID.randomUUID()
				.toString()
				.replace("-", "");
	}
}
