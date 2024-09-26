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

package com.backstage.app.attachment.service.store;

import com.backstage.app.attachment.configuration.properties.AttachmentProperties;
import com.backstage.app.attachment.model.domain.Attachment;
import com.backstage.app.exception.AppException;
import com.backstage.app.model.other.exception.ApiStatusCodeImpl;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public interface AttachmentStore
{
	AttachmentProperties.StoreType getType();

	boolean attachmentExists(Attachment attachment);

	Resource getAttachment(Attachment attachment);

	Resource getAttachment(Attachment attachment, long offset, Long length);

	default void saveAttachment(Attachment attachment, byte[] data)
	{
		saveAttachment(attachment, new ByteArrayInputStream(data));
	}

	void saveAttachment(Attachment attachment, InputStream stream);

	default void saveAttachment(Attachment attachment, Resource resource)
	{
		try
		{
			saveAttachment(attachment, resource.getInputStream());
		}
		catch (IOException e)
		{
			throw new AppException(ApiStatusCodeImpl.ATTACHMENT_ADD_ERROR, e);
		}
	}

	void deleteAttachment(Attachment attachment);
}
