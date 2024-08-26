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

package com.backstage.app.api.swagger;

import com.backstage.app.api.TestSwaggerApplication;
import com.backstage.app.api.configuration.properties.ApiProperties;
import com.backstage.app.configuration.properties.AppProperties;
import org.junit.jupiter.api.Test;
import org.springdoc.webmvc.api.OpenApiWebMvcResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = OpenApiWebMvcResource.class)
@ContextConfiguration(classes = {TestSwaggerApplication.class})
class SwaggerEnabledTest
{
	@Autowired
	private MockMvc mvc;

	@Autowired
	private AppProperties appProperties;

	@Test
	void testSwaggerIsEnabled() throws Exception
	{
		mvc.perform(get("/v3/api-docs"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(content().string(containsString(appProperties.getModule())))
				.andExpect(content().string(containsString(appProperties.getVersion())));
	}

	@DynamicPropertySource
	static void swaggerProperties(DynamicPropertyRegistry registry)
	{
		registry.add(ApiProperties.SWAGGER_ACTIVATION_PROPERTY, () -> "true");
	}
}