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

package com.backstage.app.attachment.service;

import com.backstage.app.attachment.AbstractTests;
import com.backstage.app.attachment.repository.AttachmentRepository;
import com.backstage.app.attachment.service.job.CheckAttachmentsJob;
import com.backstage.app.attachment.service.job.DeleteUnboundAttachmentsJob;
import com.backstage.app.jobs.service.JobManager;
import com.backstage.app.model.other.user.UserInfo;
import com.backstage.app.utils.TimeUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

public class AttachmentJobTests extends AbstractTests
{
	@Autowired private AttachmentRepository attachmentRepository;

	@Autowired private AttachmentService attachmentService;

	@Autowired private JobManager jobManager;

	@Value("classpath:attachment.png")
	private Resource fileResource;

	@Test
	public void checkAttachmentsJob() throws Exception
	{
		var bytes = IOUtils.toByteArray(fileResource.getInputStream());

		attachmentService.addAttachment(Objects.requireNonNull(fileResource.getFilename()), MediaType.IMAGE_PNG_VALUE, UserInfo.SYSTEM_USER_ID, bytes);
		attachmentService.addAttachment(Objects.requireNonNull(fileResource.getFilename()), MediaType.IMAGE_PNG_VALUE, UserInfo.SYSTEM_USER_ID, bytes);

		var jobResult = jobManager.executeJobAndWait(CheckAttachmentsJob.class);
		var jobResultProperties = jobResult.getProperties();

		assertNotNull(jobResultProperties.get("attachmentCount"));
		assertNotNull(jobResultProperties.get("attachmentTotalSize"));
		assertNotNull(jobResultProperties.get("unboundAttachmentCount"));
	}

	@Test
	public void checkDeleteUnboundAttachmentsJob() throws Exception
	{
		var bytes = IOUtils.toByteArray(fileResource.getInputStream());

		attachmentService.addAttachment(Objects.requireNonNull(fileResource.getFilename()), MediaType.IMAGE_PNG_VALUE, UserInfo.SYSTEM_USER_ID, bytes);
		attachmentService.addAttachment(Objects.requireNonNull(fileResource.getFilename()), MediaType.IMAGE_PNG_VALUE, UserInfo.SYSTEM_USER_ID, bytes);

		assertTrue(attachmentRepository.count() > 0);

		TimeUtils.sleepSeconds(1);

		var jobResult = jobManager.executeJobAndWait(DeleteUnboundAttachmentsJob.class);
		var jobResultProperties = jobResult.getProperties();

		assertTrue((Integer) jobResultProperties.get("processed") > 0);

		assertEquals(0, attachmentRepository.count());
	}
}
