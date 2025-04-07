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

import com.backstage.app.dict.api.model.dto.data.DictItemRemoteDto;
import com.backstage.app.dict.conversion.dto.data.DictItemConverter;
import com.backstage.app.dict.data.TestDictDataFactory;
import com.backstage.app.dict.domain.DictItem;
import lombok.SneakyThrows;
import org.geojson.FeatureCollection;
import org.geojson.GeoJsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class CommonDictDataConversionTest extends CommonDictDataServiceTest
{
	@Autowired
	private DictItemConverter itemConverter;

	protected void convertItemsWithNullData()
	{
		var dictId =  testDictFactory.createNewDict(getDictId()).getId();
		var dataMap = Collections.singletonMap(TestDictDataFactory.DOUBLE_FIELD, null);
		testDictDataFactory.createDefaultItemWithCustomField(dictId, dataMap);

		var actualDto = dictDataService.getByFilter(dictId, List.of("*"), "doubleField = null", Pageable.unpaged())
				.getContent()
				.stream()
				.map(this::mappedDto)
				.toList();

		actualDto.forEach(it -> assertNull(it.getData().get(TestDictDataFactory.DOUBLE_FIELD)));

		testDictFactory.eraseDict(dictId);
	}

	protected void convertItemsWithEmptyArray()
	{
		var dictId =  testDictFactory.createNewDict(getDictId()).getId();
		var dataMap = Collections.singletonMap(TestDictDataFactory.TIMESTAMP_FIELD, null);
		testDictDataFactory.createDefaultItemWithCustomField(dictId, dataMap);

		var actualDto = dictDataService.getByFilter(dictId, List.of("*"), null, Pageable.unpaged())
				.getContent()
				.stream()
				.filter(it -> ((Collection<?>) it.getData().get(TestDictDataFactory.TIMESTAMP_FIELD)).isEmpty())
				.map(this::mappedDto)
				.toList();

		actualDto.forEach(it -> assertTrue(((Collection<?>) it.getData().get(TestDictDataFactory.TIMESTAMP_FIELD)).isEmpty()));

		testDictFactory.eraseDict(dictId);
	}

	protected void convertItemsWithReferenceDict()
	{
		var dictId =  testDictFactory.createNewDict(getDictId()).getId();
		var refDictId =  testDictFactory.createReferenceDict(dictId).getId();
		var dictItemId = testDictDataFactory.createDefaultItem(dictId).getId();
		var refDataMap = new HashMap<String, Object>() {{
			put(dictId, dictItemId);
		}};
		testDictDataFactory.createItemWithCustomFields(refDictId, refDataMap);

		var actualDto = dictDataService.getByFilter(refDictId, List.of("*", dictId + ".timestampField"), null, PageRequest.of(0, 10))
				.getContent()
				.stream()
				.map(this::mappedDto)
				.toList();

		assertEquals(DictItemRemoteDto.class, actualDto.get(0).getData().get(dictId).getClass());

		testDictFactory.eraseDictAndRefDict(dictId, refDictId);
	}

	@SneakyThrows
	protected void convertItemsWithGeoJsonObject()
	{
		var dictId = testDictFactory.createGeoJsonDict(getDictId()).getId();

		var geo = objectMapper.readValue("{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"Point\",\"coordinates\":[37.412284,55.603515]}},{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[37.413423,55.604283],[37.41255,55.60361],[37.413995,55.602974],[37.414842,55.603629],[37.413423,55.604283]]]}},{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"LineString\",\"coordinates\":[[37.411999,55.603159],[37.413568,55.602384],[37.41522,55.603516]]}}]}",
				GeoJsonObject.class);
		var geoJson = objectMapper.writeValueAsString(geo);
		var dataMap = new HashMap<String, Object>() {{
			put("geoJsonField", geoJson);
			put("geoJsonMultivaluedField", List.of(geoJson, geoJson));
			put("stringField", "geoJsonTest");
		}};

		dictDataService.create(buildDictDataItem(dictId, dataMap));

		var actualDto = dictDataService.getByFilter(dictId, List.of("*"), "stringField = 'geoJsonTest'", Pageable.unpaged())
				.getContent()
				.stream()
				.map(this::mappedDto)
				.toList();

		assertEquals(FeatureCollection.class, actualDto.get(0).getData().get("geoJsonField").getClass());

		testDictFactory.eraseDict(dictId);
	}

	@SneakyThrows
	private DictItemRemoteDto mappedDto(DictItem dictItem)
	{
		var config = DictItemConverter.Configuration
				.builder()
				.targetClass(DictItemRemoteDto.class)
				.build();

		return objectMapper.readValue(objectMapper.writeValueAsString(itemConverter.convert(dictItem, config)), DictItemRemoteDto.class);
	}
}
