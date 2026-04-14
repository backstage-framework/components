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

import com.backstage.app.report.model.ReportFileType;
import com.backstage.app.report.model.ReportMessage;
import com.backstage.app.report.model.ReportStatus;
import com.backstage.app.report.model.ReportType;
import com.backstage.app.report.model.filter.ReportFilter;
import com.backstage.app.report.model.task.ReportTask;
import com.backstage.app.report.service.queue.ReportQueueService;
import com.backstage.app.report.service.store.ReportStore;
import com.backstage.app.report.service.task.ReportTaskManager;
import com.backstage.app.utils.ClassUtils;
import com.backstage.app.utils.MimeTypeUtils;
import com.backstage.app.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;

import java.util.concurrent.CompletableFuture;

@Slf4j
public abstract class AbstractReportService implements ReportService
{
	protected static final String REPORT_ATTACHMENT_BINDING_KEY = "REPORT_TASK";

	@Autowired protected ReportTaskManager reportTaskManager;
	@Autowired protected ReportQueueService reportQueueService;

	@Autowired protected GeneratorLocator generatorLocator;

	protected abstract ReportStore getReportStore();

	protected abstract void sendMessage(ReportMessage message);

	@Override
	public ReportTask generate(ReportType type)
	{
		return generate(type, SecurityUtils.getCurrentUserId());
	}

	@Override
	public ReportTask generate(ReportType type, String userId)
	{
		return generate(type, createEmptyFilter(type), userId);
	}

	@Override
	public ReportTask generate(ReportType type, ReportFilter filter)
	{
		return generate(type, filter, SecurityUtils.getCurrentUserId());
	}

	@Override
	public ReportTask generate(ReportType type, ReportFilter filter, String userId)
	{
		log.info("От пользователя '{}' получен запрос на генерацию отчёта: {}.", userId, filter);

		var reportFileName = this.generatorLocator.getGenerator(filter.getReportType()).generateReportFileName(filter);
		var mimeType = MimeTypeUtils.extensionToMimeType(FilenameUtils.getExtension(reportFileName));

		var task = reportTaskManager.create(userId, filter, reportFileName);

		reportQueueService.executeReportRequestMessage(new ReportMessage(filter, task.getId(), userId, reportFileName, mimeType));

		return task;
	}

	@Async
	@Override
	public CompletableFuture<byte[]> generateAsync(ReportType type)
	{
		return generateAsync(type, createEmptyFilter(type));
	}

	@Async
	@Override
	public CompletableFuture<byte[]> generateAsync(ReportType type, ReportFilter filter)
	{
		try
		{
			log.info("Начало генерации отчета {}.", type);

			byte[] data = generatorLocator.getGenerator(type)
					.generate(filter);

			log.info("Завершение генерации отчета {}.", type);

			return CompletableFuture.completedFuture(data);
		}
		catch (Exception e)
		{
			log.error("Ошибка генерации отчета {}.", type, e);

			return CompletableFuture.failedFuture(e);
		}
	}

	protected ReportFilter createEmptyFilter(ReportType type)
	{
		var filter = ClassUtils.newInstance(type.getFilterType());
		filter.setReportType(type);

		return filter;
	}

	protected void runInternal(ReportMessage message)
	{
		var reportStore = getReportStore();
		var task = reportTaskManager.getById(message.getTaskId());

		var report = reportStore.create(message.getReportFileName(), message.getMimeType(), new byte[]{});
		reportStore.linkReportWithObject(report.getId(), REPORT_ATTACHMENT_BINDING_KEY, task.getId());

		sendMessage(message);

		var reportType = message.getReportFilter()
				.getReportType();

		log.info("Отчет {} добавлен в очередь генерации.", reportType);
	}

	protected void updateReportTask(String taskId, boolean success, byte[] data, ReportFileType reportFileType)
	{
		var reportStore = getReportStore();

		if (success)
		{
			var reportId = reportStore.getIdByLinkedObject(REPORT_ATTACHMENT_BINDING_KEY, taskId);
			var report = reportStore.getById(reportId);

			reportStore.update(reportId, report.getFileName(), reportFileType.getMimeType(), data);

			reportTaskManager.complete(taskId, ReportStatus.READY, reportId);

			return;
		}

		reportStore.release(REPORT_ATTACHMENT_BINDING_KEY, taskId);

		reportTaskManager.complete(taskId, ReportStatus.ERROR, null);
	}
}
