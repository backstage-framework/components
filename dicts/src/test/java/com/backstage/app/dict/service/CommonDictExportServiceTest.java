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

import com.backstage.app.dict.api.constant.ExportedDictFormat;
import com.backstage.app.dict.common.CommonTest;
import com.backstage.app.dict.service.export.DictExportService;
import com.backstage.app.dict.service.imp.ImportCsvService;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class CommonDictExportServiceTest extends CommonTest
{
	protected static String TESTABLE_DICT_ID;

	@Autowired
	private DictExportService dictExportService;

	@Autowired
	private ImportCsvService importCsvService;

	public void buildTestableDictHierarchy(String storageDictId)
	{
		TESTABLE_DICT_ID = createNewDict(storageDictId + "export").getId();

		addDictData(TESTABLE_DICT_ID);
	}

	@Test
	public void exportJsonNotNullItemsIds()
	{
		var exportedResource = dictExportService.exportToResource(TESTABLE_DICT_ID, ExportedDictFormat.JSON, List.of("stringField"));

		assertTrue(exportedResource.getResource().exists());
		assertTrue(StringUtils.hasText(exportedResource.getFilename()));
	}

	@Test
	public void exportJsonNullItemsIds()
	{
		var exportedResource = dictExportService.exportToResource(TESTABLE_DICT_ID, ExportedDictFormat.JSON, null);

		assertTrue(exportedResource.getResource().exists());
		assertTrue(StringUtils.hasText(exportedResource.getFilename()));
	}

	@Test
	public void exportJsonNullDictIdNullItemsIds()
	{
		assertThrows(NullPointerException.class, () -> dictExportService.exportToResource(null, ExportedDictFormat.JSON, null));
	}

	@Test
	public void exportSqlNotNullItemsIds()
	{
		var exportedResource = dictExportService.exportToResource(TESTABLE_DICT_ID, ExportedDictFormat.SQL, List.of("stringField"));

		assertTrue(exportedResource.getResource().exists());
		assertTrue(StringUtils.hasText(exportedResource.getFilename()));
	}

	@Test
	public void exportSqlNullItemsIds()
	{
		var exportedResource = dictExportService.exportToResource(TESTABLE_DICT_ID, ExportedDictFormat.SQL, null);

		assertTrue(exportedResource.getResource().exists());
		assertTrue(StringUtils.hasText(exportedResource.getFilename()));
	}

	@Test
	public void exportCvsNotNullItemsIds()
	{
		var exportedResource = dictExportService.exportToResource(TESTABLE_DICT_ID, ExportedDictFormat.CSV, List.of("stringField"));

		assertTrue(exportedResource.getResource().exists());
		assertTrue(StringUtils.hasText(exportedResource.getFilename()));
	}

	@Test
	public void exportCvsNullItemsIds()
	{
		var exportedResource = dictExportService.exportToResource(TESTABLE_DICT_ID, ExportedDictFormat.CSV, null);

		assertTrue(exportedResource.getResource().exists());
		assertTrue(StringUtils.hasText(exportedResource.getFilename()));
	}

	@Test
	public void exportMultiValuedColumnToCsv()
	{
		var dictId = "testMultivaluedColumnImport";

		try (var fileStream = new ClassPathResource("testMultivaluedColumnImport.csv").getInputStream())
		{
			importCsvService.importDict(dictId, fileStream);

			var exportedResource = dictExportService.exportToResource(dictId, ExportedDictFormat.CSV, List.of());

			try (var inputStream = exportedResource.getResource().getInputStream())
			{
				var data = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

				assertTrue(StringUtils.hasText(data));
				assertTrue(data.contains("strVal1 with comma ,"));
				assertTrue(data.contains("1,2,3,4"));
				assertTrue(data.contains("5,6,7,8"));
				assertTrue(data.contains("single value, with comma 1"));
				assertTrue(data.contains("single value, with comma 2"));
			}
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}
}
