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
import com.backstage.app.api.configuration.properties.ApiProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = SimpleTestController.class)
@ContextConfiguration(classes = {TestMvcApplication.class})
class GlobalMvcExceptionHandlerUnknownError200Test
{
	@Autowired
	private MockMvc mvc;

	@Autowired
	private ApiProperties apiProperties;

	@Test
	void testExceptionThrow() throws Exception
	{
		mvc.perform(get("/test/exception"))
				.andDo(print()).andExpect(status().isOk())
				.andExpect(content().string(containsString("java.lang.Exception")))
				.andExpect(content().string(containsString("exceptionCode")));
	}

	@Test
	void testExceptionThrowWithoutStack() throws Exception
	{
		apiProperties.setStackTraceOnError(false);

		mvc.perform(get("/test/exception"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("exceptionCode")))
				.andExpect(jsonPath("$.stackTrace").doesNotExist());
	}

	@DynamicPropertySource
	static void apiProperties(DynamicPropertyRegistry registry)
	{
		registry.add(ApiProperties.HTTP_OK_ON_UNKNOWN_ERROR_PROPERTY, () -> "true");
	}
}