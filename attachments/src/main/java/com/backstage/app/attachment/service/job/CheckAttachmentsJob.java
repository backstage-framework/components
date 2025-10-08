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

package com.backstage.app.attachment.service.job;

import com.backstage.app.attachment.configuration.properties.AttachmentProperties;
import com.backstage.app.attachment.repository.AttachmentRepository;
import com.backstage.app.jobs.model.dto.other.JobResult;
import com.backstage.app.jobs.model.dto.param.EmptyJobParams;
import com.backstage.app.jobs.service.AbstractManualJob;
import com.backstage.app.jobs.service.JobDescription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty(AttachmentProperties.ACTIVATION_PROPERTY)
@JobDescription("Получение метрик хранилища вложений")
@RequiredArgsConstructor
public class CheckAttachmentsJob extends AbstractManualJob<EmptyJobParams>
{
	private final AttachmentRepository attachmentRepository;

	@Override
	protected JobResult execute()
	{
		return JobResult.ok(Map.of(
				"attachmentCount", attachmentRepository.count(),
				"attachmentTotalSize", DataSize.ofBytes(attachmentRepository.countTotalSize()).toMegabytes() + " MB",
				"unboundAttachmentCount", attachmentRepository.countUnbound()
		));
	}
}
