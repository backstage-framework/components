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
import com.backstage.app.exception.AppException;
import com.backstage.app.model.other.user.UserInfo;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public abstract class AbstractStoreTests extends AbstractTests
{
	@Autowired private TransactionTemplate transactionTemplate;

	@Autowired private AttachmentService attachmentService;

	@Value("classpath:attachment.png")
	private Resource fileResource;

	private static String attachmentId;

	@Order(1)
	@Test
	public void addAttachment() throws Exception
	{
		var bytes = IOUtils.toByteArray(fileResource.getInputStream());

		var attachment = attachmentService.addAttachment(Objects.requireNonNull(fileResource.getFilename()), MediaType.IMAGE_PNG_VALUE, UserInfo.SYSTEM_USER_ID, bytes);
		var savedData = attachmentService.getAttachmentData(attachment.getId());

		assertEquals(attachment.getSize(), bytes.length);
		assertArrayEquals(bytes, IOUtils.toByteArray(savedData.getInputStream()));

		attachmentId = attachment.getId();
	}

	@Order(2)
	@Test
	public void getAttachment() throws Exception
	{
		var attachment = attachmentService.getAttachment(attachmentId);
		var dataResource = attachmentService.getAttachmentData(attachment.getId());
		var dataChecksum = attachmentService.calculateChecksum(IOUtils.toByteArray(dataResource.getInputStream()));

		assertEquals(attachment.getChecksum(), dataChecksum);
	}

	@Order(3)
	@Test
	public void syncStores() throws Exception
	{
		var source = attachmentService.getAttachmentStore();
		var target = attachmentService.getAttachmentStores().stream().filter(it -> !it.equals(source)).findFirst().orElse(null);

		assertNotNull(target);

		var attachment = attachmentService.getAttachment(attachmentId);

		assertThrows(AppException.class, () -> target.getAttachment(attachment));

		attachmentService.syncAttachmentStores(source.getType(), target.getType());

		var dataResource = target.getAttachment(attachment);
		var dataChecksum = attachmentService.calculateChecksum(IOUtils.toByteArray(dataResource.getInputStream()));

		assertEquals(attachment.getChecksum(), dataChecksum);
	}

	@Order(4)
	@Test
	public void deleteAttachment()
	{
		var attachment = attachmentService.getAttachment(attachmentId);
		var attachmentStore = attachmentService.getAttachmentStore();

		attachmentStore.deleteAttachment(attachment);

		assertFalse(attachmentStore.attachmentExists(attachment));
	}

	@Order(5)
	@Test
	public void addAttachmentWithRollback() throws Exception
	{
		var bytes = IOUtils.toByteArray(fileResource.getInputStream());
		var rolledBackAttachmentId = UUID.randomUUID().toString();
		MutableObject<Attachment> attachment = new MutableObject<>();

		try
		{
			transactionTemplate.execute(status -> {
				attachment.setValue(attachmentService.addAttachment(rolledBackAttachmentId, Objects.requireNonNull(fileResource.getFilename()), MediaType.IMAGE_PNG_VALUE, UserInfo.SYSTEM_USER_ID, bytes));

				throw new RuntimeException("rolling back");
			});
		}
		catch (Exception ignore)
		{
			// ignore
		}

		assertFalse(attachmentService.getAttachmentStore().attachmentExists(attachment.getValue()));
	}
}
