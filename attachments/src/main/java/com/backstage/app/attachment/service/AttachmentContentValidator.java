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

import com.backstage.app.exception.AppException;
import com.backstage.app.model.other.exception.ApiStatusCodeImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.io.TikaInputStream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "app.attachments.verify-content")
@Slf4j
public class AttachmentContentValidator implements AttachmentServiceAdvice
{
	private static final Integer FILE_LENGTH_READ = 16 * 1024;

	@Override
	public void handleAddAttachment(String id, String fileName, String mimeType, String userId, Resource resource)
	{
		var tika = new Tika();

		try (var inputStream = TikaInputStream.get(resource.getInputStream()))
		{
			tika.setMaxStringLength(FILE_LENGTH_READ);

			var detectedMimeType = tika.detect(inputStream);

			if (detectedMimeType.equals(mimeType))
			{
				return;
			}

			var errorMessage = ApiStatusCodeImpl.ATTACHMENT_INVALID_CONTENT.getMessage()
					.formatted(id, mimeType, detectedMimeType);

			throw new AppException(ApiStatusCodeImpl.ATTACHMENT_INVALID_CONTENT, errorMessage);
		}
		catch (IOException e)
		{
			log.error("При проверке mimeType файла с id = {} произошла ошибка.", id, e);

			throw new AppException(ApiStatusCodeImpl.ATTACHMENT_READ_ERROR);
		}
	}
}
