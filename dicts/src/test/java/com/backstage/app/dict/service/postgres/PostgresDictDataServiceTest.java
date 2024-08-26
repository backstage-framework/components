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

package com.backstage.app.dict.service.postgres;

import com.backstage.app.dict.common.TestPipeline;
import com.backstage.app.dict.service.CommonDictDataServiceTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Order(TestPipeline.POSTGRES_DICT_DATA)
@PostgresStorage
public class PostgresDictDataServiceTest extends CommonDictDataServiceTest
{
	@BeforeAll
	public void createPostgresTestableHierarchy()
	{
		initDictDataTestableHierarchy(POSTGRES_DICT_ID);
	}

	@Test
	void getByIdsCorrect()
	{
		getByIds();
	}

	@Test
	void getByFilterWithNullRefFieldCorrect()
	{
		getByFilterWithNullRefField();
	}

	@Test
	@Order(TestPipeline.DICT_DATA_GET_BY_FILTER_TEST)
	void getByFilterCorrect()
	{
		getByFilter();
	}

	@Test
	@Order(TestPipeline.DICT_DATA_GET_BY_FILTER_WITH_LOGICAL_EXPRESSION_TEST)
	void getByFilterWithLogicalExpressionCorrect()
	{
		getByFilterWithLogicalExpression();
	}

	@Test
	void getIdsByFilterCorrect()
	{
		getIdsByFilter();
	}

	@Test
	void getByFilter_withRefCorrect()
	{
		getByFilterWithDictReference();
	}

	@Test
	void getByFilter_withRefAllFieldSelectCorrect()
	{
		getByFilterWithDictReferenceAllFieldSelect();
	}

	@Test
	void getByFilter_withQueryRefCorrect()
	{
		getByFilterWithQueryReference();
	}

	@Test
	void getByFilter_withArrayContainsAnyValue()
	{
		getByFilterWithArrayContainsAnyValue();
	}

	@Test
	void getByFilter_withArrayContainsAllValue()
	{
		getByFilterWithArrayContainsAllValue();
	}

	@Test
	void existsByIdCorrect()
	{
		existsById();
	}

	@Test
	@Order(TestPipeline.DICT_DATA_EXISTS_BY_FILTER_TEST)
	void existsByFilterCorrect()
	{
		existsByFilter();
	}

	@Test
	void check_attachmentBindingWithCreateDictItem()
	{
		attachmentBindingWithCreateDictItem();
	}

	@Test
	void check_attachmentBindingWithUpdateDictItem()
	{
		checkAttachmentBindingWithUpdateDictItem();
	}

	@Test
	void check_attachmentReleaseWithDeleteDictItem()
	{
		checkAttachmentReleaseWithDeleteDictItem();
	}

	@Test
	void check_attachmentBindingDeleteDictItem()
	{
		checkAttachmentBindingWithDeleteDictItem();
	}

	@Test
	void create()
	{
		createDictItem();
	}

	@Test
	void create_withDifferentType()
	{
		createWithDifferentType();
	}

	@Test
	void create_withUUIDDictFieldStartNumericIds()
	{
		createWithUUIDDictFieldStartNumericIds();
	}

	@Test
	void create_correctContainsFieldsInHistoryMap()
	{
		createCorrectContainsFieldsInHistoryMap();
	}

	@Test
	void create_withJson()
	{
		createDictItemWithJson();
	}

	@Test
	void create_GeoJson()
	{
		createDictItemWithGeoJsonObject();
	}

	@Test
	void update()
	{
		updateDictItem();
	}

	@Test
	void update_withEmptyMultivaluedData()
	{
		updateDictItemWithEmptyMultivaluedData();
	}

	@Test
	void update_withJson()
	{
		updateDictItemWithJson();
	}

	@Test
	void update_withGeoJson()
	{
		updateDictItemWithGeoJson();
	}

	@Test
	void update_concurrentExc()
	{
		updateConcurrentExc();
	}

	@Test
	void delete()
	{
		deleteDictItem();
	}

	@ParameterizedTest
	@ValueSource(strings = {"Test reason", "1234"})
	void delete_withReason(String reason)
	{
		deleteWithReason(reason);
	}

	@Test
	void delete_withEmptyReason()
	{
		deleteWithEmptyReason();
	}

	@Test
	void createMany()
	{
		createManyDictItems();
	}

	@Test
	void create_withNullData()
	{
		createDictItemWithNullData();
	}

	@Test
	void getByFilter_innerDictSort()
	{
		getByFilterInnerDictSort();
	}

	@Test
	void getByFilter_innerDictSortWrongFiledName()
	{
		getByFilterInnerDictSortWrongFiledName();
	}

	@Test
	void getByFilter_dictSortServiceField()
	{
		getByFilterDictSortServiceField();
	}

	@Test
	void getByFilter_dictSortDataField()
	{
		getByFilterDictSortDataField();
	}

	@Test
	void getByFilter_dictMultiplySortDataField()
	{
		getByFilterWithMultipleSortDataField();
	}

	@Test
	void getByFilter_withServiceSelectField()
	{
		getByFilterWithServiceSelectField();
	}

	@Test
	void getByFilter_withDifferentDateCorrect()
	{
		getByFilterWithDifferentDateCorrect();
	}

	@Test
	void createDictItemWithDefaultFields()
	{
		createDictItemWithDefaultFields("testDict2");
	}

	@Test
	void createDictItemWithIncorrectStringFieldSize()
	{
		createDictItemWithIncorrectStringFieldSize("testDict2");
	}

	@Test
	void createDictItemWithIncorrectIntegerFieldSize()
	{
		createDictItemWithIncorrectIntegerFieldSize("testDict2");
	}

	@Test
	void createDictItemWithIncorrectDecimalFieldSize()
	{
		createDictItemWithIncorrectDecimalFieldSize("testDict2");
	}
}
