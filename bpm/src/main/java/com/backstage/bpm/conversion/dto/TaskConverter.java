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

package com.backstage.bpm.conversion.dto;

import com.backstage.app.conversion.dto.AbstractConverter;
import com.backstage.app.utils.DateUtils;
import com.backstage.bpm.domain.Task;
import com.backstage.bpm.model.dto.TaskDto;
import org.springframework.stereotype.Component;

@Component
public class TaskConverter extends AbstractConverter<Task, TaskDto>
{
	@Override
	public TaskDto convert(Task source)
	{
		var target = new TaskDto();

		target.setId(source.getId());
		target.setName(source.getName());
		target.setComment(source.getComment());
		target.setType(source.getType());
		target.setStatus(source.getStatus());
		target.setDeadline(DateUtils.toZonedDateTime(source.getDeadline()));
		target.setCreated(DateUtils.toZonedDateTime(source.getCreated()));
		target.setUpdated(DateUtils.toZonedDateTime(source.getUpdated().toLocalDateTime()));
		target.setCompleted(DateUtils.toZonedDateTime(source.getCompleted()));

		return target;
	}
}
