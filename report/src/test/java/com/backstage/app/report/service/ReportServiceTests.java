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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.backstage.app.report.AbstractTests;
import com.backstage.app.report.model.ExampleReportType;
import com.backstage.app.report.model.ReportMessage;
import com.backstage.app.report.model.filter.SimpleReportFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ReportServiceTests extends AbstractTests
{
	private static final String USER_ID = "system";
	private static final String TASK_ID = "taskId";
	private static final String MIME_TYPE = "mimeType";

	@Autowired private ReportService reportService;

	@Autowired private ObjectMapper objectMapper;

	@Test
	public void generateExampleXlsCorrectTest()
	{
		var filter = SimpleReportFilter.builder()
				.reportType(ExampleReportType.EXAMPLE_1)
				.from(FROM)
				.to(TO)
				.build();

		assertDoesNotThrow(() -> reportService.generate(ExampleReportType.EXAMPLE_1, filter, USER_ID));
	}

	@Test
	public void serializeCorrectReportMessageTest()
	{
		var filter = SimpleReportFilter.builder()
				.reportType(ExampleReportType.EXAMPLE_1)
				.from(FROM)
				.to(TO)
				.build();

		var reportMessage = new ReportMessage();
		reportMessage.setReportFilter(filter);
		reportMessage.setMimeType(MIME_TYPE);
		reportMessage.setTaskId(TASK_ID);
		reportMessage.setUserId(USER_ID);

		assertDoesNotThrow(() -> objectMapper.writeValueAsString(reportMessage));
	}

	@Test
	public void deserializeCorrectReportMessageTest()
	{
		var correctSerializedReportMessage = "{\"reportFilter\":{\"@c\":\"com.backstage.app.report.model.filter.SimpleReportFilter\",\"reportType\":[\"com.backstage.app.report.model.ExampleReportType\",\"EXAMPLE_1\"]}}";

		assertDoesNotThrow(() -> objectMapper.readValue(correctSerializedReportMessage, ReportMessage.class));
	}

	@Test
	public void DeserializeWrongReportMessageTest()
	{
		var wrongSerializedReportMessage = "{\"reportFilter\":{\"reportType\":\"EXAMPLE_XLS_1\"}}";

		assertThrows(JsonProcessingException.class, () -> objectMapper.readValue(wrongSerializedReportMessage, ReportMessage.class));
	}
}
