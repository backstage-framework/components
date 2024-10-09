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

import com.backstage.app.dict.common.CommonTest;
import com.backstage.app.dict.service.imp.ImportCsvService;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CommonImportCsvServiceTest extends CommonTest
{
	public static final String TEST_MULTIVALUED_COLUMN_IMPORT = "testMultivaluedColumnImport";
	@Autowired
	private ImportCsvService importCsvService;

	protected void importDict()
	{
		try (var fileStream = new ClassPathResource("testMultivaluedColumnImport.csv").getInputStream())
		{
			var result = importCsvService.importDict(TEST_MULTIVALUED_COLUMN_IMPORT, fileStream);

			assertEquals(10, result.size());
			assertEquals(1L, (Long) result.get(0).getData().get("field1"));
			assertIterableEquals(List.of("1", "2", "3", "4"), (List<String>) result.get(0).getData().get("field2"));
			assertIterableEquals(List.of(1L, 2L, 3L, 4L), (List<Long>) result.get(0).getData().get("field3"));
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}
}
