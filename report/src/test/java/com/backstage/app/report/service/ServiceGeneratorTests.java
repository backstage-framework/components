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

package com.backstage.app.report.service;

import com.backstage.app.report.AbstractTests;
import com.backstage.app.report.model.ExampleReportType;
import com.backstage.app.report.model.filter.SimpleReportFilter;
import com.backstage.app.report.utils.ReportUtils;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ServiceGeneratorTests extends AbstractTests
{
	@Autowired private GeneratorLocator generatorLocator;

	@Test
	public void exampleXls1GeneratorFileNameFullFilterTest()
	{
		var filter = SimpleReportFilter.builder()
				.reportType(ExampleReportType.EXAMPLE_1)
				.from(FROM)
				.to(TO)
				.build();

		var expectedFileName = ReportUtils.getReportFileName(filter);

		var fileName = generatorLocator.getGenerator(filter.getReportType()).generateReportFileName(filter);

		assertEquals(expectedFileName, fileName);
	}

	@Test
	public void exampleXls1GeneratorTest() throws IOException
	{
		var filter = SimpleReportFilter.builder()
				.reportType(ExampleReportType.EXAMPLE_1)
				.from(FROM)
				.to(TO)
				.build();

		var workbook = new SXSSFWorkbook();

		var outputStream = new ByteArrayOutputStream();
		workbook.write(outputStream);
		byte[] expectedData = outputStream.toByteArray();

		byte[] data = generatorLocator.getGenerator(filter.getReportType()).generate(filter);

//		assertEquals(expectedData.length, data.length);
	}
}
