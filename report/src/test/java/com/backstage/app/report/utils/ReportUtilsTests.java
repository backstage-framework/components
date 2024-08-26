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

package com.backstage.app.report.utils;

import com.backstage.app.report.AbstractTests;
import com.backstage.app.report.model.ExampleReportType;
import com.backstage.app.report.model.filter.SimpleReportFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ReportUtilsTests extends AbstractTests
{
	private static final  String SHORT_TEMPLATE = "%s за %s%s";
	private static final  String FULL_TEMPLATE = "%s за %s%s%s%s";
	private static final LocalDate FROM = LocalDate.of(2000, 1, 1);
	private static final LocalDate TO = LocalDate.of(2000, 12, 31);

	@Test
	public void checkFileNameMethodFullFilterTest()
	{
		var repotType = ExampleReportType.EXAMPLE_1;

		var filter = SimpleReportFilter.builder()
				.reportType(repotType)
				.from(FROM)
				.to(TO)
				.build();

		var expectedFileName = FULL_TEMPLATE.formatted(
				repotType.getTitle(),
				ReportDateUtils.LOCAL_DATE_FORMATTER.format(FROM),
				ReportUtils.HYPHEN,
				ReportDateUtils.LOCAL_DATE_FORMATTER.format(TO),
				repotType.getReportFileType().getExtension());

		var fileName = ReportUtils.getReportFileName(filter);

		assertEquals(expectedFileName, fileName);
	}

	@Test
	public void checkFileNameMethodShortFilterTest()
	{
		var repotType = ExampleReportType.EXAMPLE_1;

		var filter = SimpleReportFilter.builder()
				.reportType(repotType)
				.from(FROM)
				.to(FROM)
				.build();

		var expectedFileName = SHORT_TEMPLATE.formatted(
				repotType.getTitle(),
				ReportDateUtils.LOCAL_DATE_FORMATTER.format(FROM),
				repotType.getReportFileType().getExtension());

		var fileName = ReportUtils.getReportFileName(filter);

		assertEquals(expectedFileName, fileName);
	}
}
