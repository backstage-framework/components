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

package com.backstage.app.service.enums;

import com.backstage.app.common.AbstractTests;
import com.backstage.app.exception.ObjectNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ComponentScan(basePackageClasses = TestEnums.class)
public class ApiEnumServiceTest extends AbstractTests
{
	@Autowired
	private ApiEnumService apiEnumService;

	@Test
	void getEnumNames_success()
	{
		var expected = Set.of("TestEnumInOtherPackage", "TestEnum0", "TestEnum1");

		var actual = apiEnumService.getEnumNames();

		assertEquals(expected, actual);
	}

	@Test
	void getEnumDescription_success()
	{
		var expected = Map.of("TEST_VALUE_0", "TEST_VALUE_0", "TEST_VALUE_1", "TEST_VALUE_1");

		var actual = apiEnumService.getEnumDescription("TestEnum0");

		assertEquals(expected, actual);
	}

	@Test
	void getEnumDescription_objectNotFound()
	{
		var expected = ObjectNotFoundException.class;

		Executable actual = () -> apiEnumService.getEnumDescription("NotExistedEnum");

		assertThrows(expected, actual);
	}
}
