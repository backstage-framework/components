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

package com.backstage.app.api.controller;

import com.backstage.app.api.TestMvcApplication;
import com.backstage.app.api.controller.request.CoffeeRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = SimpleTestController.class)
@ContextConfiguration(classes = {TestMvcApplication.class})
class GlobalMvcExceptionHandlerHttpMessageNotReadableExceptionTest
{
	@Autowired
	private MockMvc mvc;

	@Autowired
	private ObjectMapper mapper;

	@Test
	void shouldReturnDefaultMessage() throws Exception
	{
		mvc.perform(get("/test/ping"))
				.andDo(print()).andExpect(status().isOk())
				.andExpect(content().string(containsString("pong")));
	}

	@Test
	void testEnumValidation_singleEnum() throws Exception
	{
		var paramName = "size";
		var param = CoffeeRequest.CupSize.SMALL.name();
		var badBody = Map.of(paramName, param);

		mvc.perform(post("/test/order")
						.contentType(MediaType.APPLICATION_JSON)
						.content(mapper.writeValueAsString(badBody)))
				.andExpect(status().isOk())
				.andExpect(jsonPath(paramName).value(is(param)))
				.andDo(print());
	}

	@Test
	void testEnumValidation_singleEnumBadBody() throws Exception
	{
		var paramName = "size";
		var badBody = Map.of(paramName, "SMALLLLLL");
		var availableValues = Arrays.stream(CoffeeRequest.CupSize.values())
				.map(Enum::name)
				.collect(Collectors.joining(", "));

		mvc.perform(post("/test/order")
						.contentType(MediaType.APPLICATION_JSON)
						.content(mapper.writeValueAsString(badBody)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("status").value(is(4)))
				.andExpect(jsonPath("message").value(containsString("Параметр %s".formatted(paramName))))
				.andExpect(jsonPath("message").value(containsString(availableValues)))
				.andDo(print());
	}

	@Test
	void testEnumValidation_listOfEnum() throws Exception
	{
		var paramName = "types";
		var param = List.of(CoffeeRequest.CoffeeType.ESPRESSO.name(), CoffeeRequest.CoffeeType.FLAT_WHITE.name());
		var body = Map.of(paramName, param);

		mvc.perform(post("/test/order")
						.contentType(MediaType.APPLICATION_JSON)
						.content(mapper.writeValueAsString(body)))
				.andExpect(status().isOk())
				.andExpect(jsonPath(paramName).value(is(param)))
				.andDo(print());
	}

	@Test
	void testEnumValidation_listOfEnumBadBody() throws Exception
	{
		var paramName = "types";
		var badBody = Map.of(paramName, List.of("ESPRESSO", "LATTE_EEE"));
		var availableValues = Arrays.stream(CoffeeRequest.CoffeeType.values())
				.map(Enum::name)
				.collect(Collectors.joining(", "));

		mvc.perform(post("/test/order")
						.contentType(MediaType.APPLICATION_JSON)
						.content(mapper.writeValueAsString(badBody)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("status").value(is(4)))
				.andExpect(jsonPath("message").value(containsString("Параметр %s".formatted(paramName))))
				.andExpect(jsonPath("message").value(containsString(availableValues)))
				.andDo(print());
	}

	@Test
	void testEnumValidation_mapOfEnum() throws Exception
	{
		var paramName = "additives";
		var param = Map.of(CoffeeRequest.Additive.MILK.name(), 1,
				CoffeeRequest.Additive.SUGAR.name(), 0);
		var body = Map.of(paramName, param);

		mvc.perform(post("/test/order")
						.contentType(MediaType.APPLICATION_JSON)
						.content(mapper.writeValueAsString(body)))
				.andExpect(status().isOk())
				.andExpect(jsonPath(paramName).value(is(param)))
				.andDo(print());
	}

	@Test
	void testEnumValidation_mapOfEnumBadBody() throws Exception
	{
		var paramName = "additives";
		var badBody = Map.of(paramName, Map.of("SUGARRR", -1, "SYRUP", -2));
		var availableValues = Arrays.stream(CoffeeRequest.Additive.values())
				.map(Enum::name)
				.collect(Collectors.joining(", "));

		mvc.perform(post("/test/order")
						.contentType(MediaType.APPLICATION_JSON)
						.content(mapper.writeValueAsString(badBody)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("status").value(is(4)))
				.andExpect(jsonPath("message").value(containsString("Параметр %s".formatted(paramName))))
				.andExpect(jsonPath("message").value(containsString(availableValues)))
				.andDo(print());
	}

	@Test
	void testEnumValidation_badInnerClassEnum() throws Exception
	{
		var paramName = "wish";
		var innerParamName = "temperature";
		var badBody = Map.of(paramName, Map.of(innerParamName, "COLDD"));
		var availableValues = Arrays.stream(CoffeeRequest.Wish.MilkTemperature.values())
				.map(Enum::name)
				.collect(Collectors.joining(", "));

		mvc.perform(post("/test/order")
						.contentType(MediaType.APPLICATION_JSON)
						.content(mapper.writeValueAsString(badBody)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("status").value(is(4)))
				.andExpect(jsonPath("message").value(containsString("Параметр %s".formatted(paramName + "." + innerParamName))))
				.andExpect(jsonPath("message").value(containsString(availableValues)))
				.andDo(print());
	}

	@Test
	void testEnumValidation_badInnerListInList() throws Exception
	{
		var paramName = "wishes";
		var innerParamName = "temperatures";
		var badBody = Map.of(paramName, List.of(Map.of(innerParamName, List.of("COLDD"))));
		var availableValues = Arrays.stream(CoffeeRequest.Wish.MilkTemperature.values())
				.map(Enum::name)
				.collect(Collectors.joining(", "));

		mvc.perform(post("/test/order")
						.contentType(MediaType.APPLICATION_JSON)
						.content(mapper.writeValueAsString(badBody)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("status").value(is(4)))
				.andExpect(jsonPath("message").value(containsString("Параметр %s".formatted(paramName + "." + innerParamName))))
				.andExpect(jsonPath("message").value(containsString(availableValues)))
				.andDo(print());
	}

	@Test
	void testEnumValidation_badInnerListInMap() throws Exception
	{
		var paramName = "wishes";
		var innerParamName = "temperatures";
		var badBody = Map.of(paramName, Map.of(Map.of(innerParamName, List.of("COLDD")), 1));

		mvc.perform(post("/test/order")
						.contentType(MediaType.APPLICATION_JSON)
						.content(mapper.writeValueAsString(badBody)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("status").value(is(4)))
				.andDo(print());
	}

	@Test
	void testTypeValidation_validInteger() throws Exception
	{
		var paramName = "fieldInteger";
		var badBody = Map.of(paramName, Integer.MAX_VALUE);

		mvc.perform(post("/test/calculate")
						.contentType(MediaType.APPLICATION_JSON)
						.content(mapper.writeValueAsString(badBody)))
				.andExpect(status().isOk())
				.andExpect(jsonPath(paramName).value(is(Integer.MAX_VALUE)))
				.andDo(print());

	}

	@Test
	void testTypeValidation_outOfRangeInteger() throws Exception
	{
		var paramName = "fieldInteger";
		var badBody = Map.of(paramName, Long.MAX_VALUE);

		mvc.perform(post("/test/calculate")
						.contentType(MediaType.APPLICATION_JSON)
						.content(mapper.writeValueAsString(badBody)))
				.andExpect(jsonPath("status").value(is(4)))
				.andDo(print());
	}

	@Test
	void testTypeValidation_badInteger() throws Exception
	{
		var paramName = "fieldInteger";
		var badBody = Map.of(paramName, "sampleInteger");

		mvc.perform(post("/test/calculate")
						.contentType(MediaType.APPLICATION_JSON)
						.content(mapper.writeValueAsString(badBody)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("message").value(containsString("Параметр %s".formatted(paramName))))
				.andExpect(jsonPath("status").value(is(4)))
				.andDo(print());
	}

	@Test
	void testTypeValidation_badDouble() throws Exception
	{
		var paramName = "fieldDouble";
		var badBody = Map.of(paramName, "sample");

		mvc.perform(post("/test/calculate")
						.contentType(MediaType.APPLICATION_JSON)
						.content(mapper.writeValueAsString(badBody)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("message").value(containsString("Параметр %s".formatted(paramName))))
				.andExpect(jsonPath("status").value(is(4)))
				.andDo(print());
	}

	@Test
	void testTypeValidation_listOfBadIntegers() throws Exception
	{
		var paramName = "integers";
		var param = List.of("one", "two");
		var badBody = Map.of(paramName, param);

		mvc.perform(post("/test/calculate")
						.contentType(MediaType.APPLICATION_JSON)
						.content(mapper.writeValueAsString(badBody)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("message").value(containsString("Параметр %s".formatted(paramName))))
				.andExpect(jsonPath("status").value(is(4)))
				.andDo(print());
	}

	@Test
	void testTypeValidation_mapOfBadNumbers() throws Exception
	{
		var paramName = "map";
		var param = Map.of("one", 1, 2, "two");
		var badBody = Map.of(paramName, param);

		mvc.perform(post("/test/calculate")
						.contentType(MediaType.APPLICATION_JSON)
						.content(mapper.writeValueAsString(badBody)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("message").value(containsString("Параметр %s".formatted(paramName))))
				.andExpect(jsonPath("status").value(is(4)))
				.andDo(print());
	}

}