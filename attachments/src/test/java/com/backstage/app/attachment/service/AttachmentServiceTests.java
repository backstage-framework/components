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

package com.backstage.app.attachment.service;

import com.backstage.app.attachment.AbstractTests;
import com.backstage.app.attachment.model.domain.Attachment;
import com.backstage.app.attachment.utils.AttachmentBindingUtils;
import com.backstage.app.model.other.user.UserInfo;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class AttachmentServiceTests extends AbstractTests
{
	@Autowired AttachmentService attachmentService;

	@Value("classpath:attachment.png")
	private Resource fileResource;

	@Test
	public void testComplexObjectIds() throws Exception
	{
		var attachment = createAttachment();
		var userId = "userId";
		var attachmentType = "TYPE_FOR_COMPLEX_OBJECT_ID";

		var firstSegment = UUID.randomUUID().toString();
		var secondSegment = UUID.randomUUID().toString();
		var thirdSegment = UUID.randomUUID().toString();

		attachmentService.bindAttachment(attachment.getId(), userId, attachmentType, AttachmentBindingUtils.buildComplexObjectId(firstSegment));
		attachmentService.bindAttachment(attachment.getId(), userId, attachmentType, AttachmentBindingUtils.buildComplexObjectId(firstSegment, secondSegment));
		attachmentService.bindAttachment(attachment.getId(), userId, attachmentType, AttachmentBindingUtils.buildComplexObjectId(firstSegment, secondSegment, thirdSegment));

		assertTrue(attachmentService.isAttachmentBound(attachment.getId(), attachmentType));

		assertEquals(attachment.getId(), attachmentService.getAttachments(attachmentType, AttachmentBindingUtils.buildObjectIdPattern(firstSegment)).get(0).getId());
		assertEquals(attachment.getId(), attachmentService.getAttachments(attachmentType, AttachmentBindingUtils.buildObjectIdPattern(firstSegment, secondSegment)).get(0).getId());
		assertEquals(attachment.getId(), attachmentService.getAttachments(attachmentType, AttachmentBindingUtils.buildObjectIdPattern(firstSegment, secondSegment, thirdSegment)).get(0).getId());

		assertEquals(attachment.getId(), attachmentService.getAttachmentIds(attachmentType, AttachmentBindingUtils.buildObjectIdPattern(firstSegment)).get(0));
		assertEquals(attachment.getId(), attachmentService.getAttachmentIds(attachmentType, AttachmentBindingUtils.buildObjectIdPattern(firstSegment, secondSegment)).get(0));
		assertEquals(attachment.getId(), attachmentService.getAttachmentIds(attachmentType, AttachmentBindingUtils.buildObjectIdPattern(firstSegment, secondSegment, thirdSegment)).get(0));

		attachmentService.releaseAttachments(attachmentType, AttachmentBindingUtils.buildComplexObjectId(firstSegment, secondSegment, thirdSegment));

		assertTrue(attachmentService.isAttachmentBound(attachment.getId(), attachmentType));

		attachmentService.releaseAttachments(attachmentType, AttachmentBindingUtils.buildObjectIdPattern(firstSegment));

		assertFalse(attachmentService.isAttachmentBound(attachment.getId(), attachmentType));
	}

	@Test
	@Transactional
	public void testBindingsInTransaction() throws Exception
	{
		testBindings();
	}

	@Test
	public void testBindingsWithoutTransaction() throws Exception
	{
		testBindings();
	}

	private void testBindings() throws Exception
	{
		var attachment = createAttachment();
		var userId = "userId";
		var attachmentType = "TEST_TYPE";
		var objectId = "objectId";

		attachmentService.bindAttachment(attachment.getId(), userId, attachmentType, objectId);

		assertTrue(attachmentService.isAttachmentBound(attachment.getId(), attachmentType));
		assertFalse(attachmentService.isAttachmentBound(attachment.getId(), attachmentType + 1));

		// Проверяем многократный bind/release, чтобы исключить проблемы с кэшем JPA.
		attachmentService.releaseAttachment(attachment.getId());

		assertFalse(attachmentService.isAttachmentBound(attachment.getId(), attachmentType));

		attachmentService.bindAttachment(attachment.getId(), userId, attachmentType, objectId);

		assertTrue(attachmentService.isAttachmentBound(attachment.getId(), attachmentType));
	}

	private Attachment createAttachment() throws Exception
	{
		var bytes = IOUtils.toByteArray(fileResource.getInputStream());

		return attachmentService.addAttachment(Objects.requireNonNull(fileResource.getFilename()), MediaType.IMAGE_PNG_VALUE, UserInfo.SYSTEM_USER_ID, bytes);
	}
}
