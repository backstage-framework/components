/*
 *    Copyright 2019-2025 the original author or authors.
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

import com.backstage.app.report.model.ReportStatus;
import com.backstage.app.report.model.task.ReportTask;
import com.backstage.app.report.model.task.SimpleReportTask;
import com.backstage.app.report.service.task.InMemoryReportTaskService;

public class TestInMemoryReportTaskService extends InMemoryReportTaskService
{
	@Override
	public ReportTask complete(String id, ReportStatus reportStatus, String reportId)
	{
		var task = (SimpleReportTask) getById(id);

		if (task == null)
		{
			return null;
		}

		task.setReportStatus(reportStatus);
		task.setReportId(reportId);

		return task;
	}
}