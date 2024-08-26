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

package com.backstage.app;

import com.backstage.app.common.AbstractTests;
import com.backstage.app.utils.RestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RestUtilsTest extends AbstractTests
{
	private static final String TEST_ADDRESS_URL = "https://1.1.1.1/";

	@Test
	public void testCorrectRequest()
	{
		var restTemplate = RestUtils.createRestTemplate(60);

		var response = restTemplate.getForEntity(TEST_ADDRESS_URL, String.class);

		assertEquals(response.getStatusCode(), HttpStatus.OK);
	}
}
