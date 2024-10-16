package com.backstage.app.dict.data;

import com.backstage.app.dict.api.model.dto.data.DictItemDto;
import com.backstage.app.dict.api.model.dto.data.DictItemRemoteDto;
import com.backstage.app.dict.common.CommonTest;
import com.backstage.app.dict.conversion.dto.data.DictItemConverter;
import com.backstage.app.dict.domain.DictItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DictItemConverterSerializationTest extends CommonTest
{
	@Autowired
	protected ObjectMapper objectMapper;

	@Autowired
	protected DictItemConverter dictItemConverter;

	@ParameterizedTest
	@MethodSource("testSource")
	void convertAndSerialize(DictItemConverter.Configuration config, Collection<String> expectedSubStrings)
	{
		DictItem dictItem = new DictItem();
		dictItem.setId("1");
		dictItem.setVersion(1L);

		var date = LocalDateTime.of(2024, 1, 1, 12, 0, 0);
		dictItem.setCreated(date);
		dictItem.setUpdated(date);

		Map<String, Object> data = Map.of(
				"stringField", "string",
				"integerField", 1L,
				"doubleField", BigDecimal.valueOf(Double.parseDouble("2.776")),
				"timestampField", "2021-08-15T06:00:00.000Z",
				"booleanField", true);

		dictItem.setData(data);

		var actual = toJson(dictItemConverter.convert(dictItem, config));

		assertSerializedValues(actual, expectedSubStrings);
	}

	private <T> String toJson(T dictItem)
	{
		try
		{
			return objectMapper.writeValueAsString(dictItem);
		}
		catch (JsonProcessingException e)
		{
			throw new RuntimeException(e);
		}
	}

	private void assertSerializedValues(String value, Collection<String> subStrings)
	{
		subStrings.forEach(subString -> assertTrue(value.contains(subString)));
	}

	private static Stream<Arguments> testSource()
	{
		var config1 = DictItemConverter.Configuration
				.builder()
				.targetClass(DictItemDto.class)
				.build();
		var result1 = List.of("\"id\":\"1\"", "\"booleanField\":true",
				"\"timestampField\":\"2021-08-15T06:00:00.000Z\"", "\"integerField\":1", "\"doubleField\":2.776",
				"\"stringField\":\"string\"", "\"version\":1", "\"created\":\"2024-01-01T12:00:00\"",
				"\"updated\":\"2024-01-01T12:00:00\"");

		var config2 = DictItemConverter.Configuration
				.builder()
				.targetClass(DictItemRemoteDto.class)
				.build();
		var result2 = List.of("\"id\":\"1\"", "\"booleanField\":[\"java.lang.Boolean\",true]",
				"\"timestampField\":[\"java.lang.String\",\"2021-08-15T06:00:00.000Z\"]",
				"\"integerField\":[\"java.lang.Long\",1]", "\"doubleField\":[\"java.math.BigDecimal\",2.776]",
				"\"stringField\":[\"java.lang.String\",\"string\"]", "\"version\":1",
				"\"created\":\"2024-01-01T12:00:00\"", "\"updated\":\"2024-01-01T12:00:00\"");

		return Stream.of(
				Arguments.of(config1, result1),
				Arguments.of(config2, result2)
		);
	}
}