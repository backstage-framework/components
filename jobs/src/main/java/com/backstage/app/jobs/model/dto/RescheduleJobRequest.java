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

package com.backstage.app.jobs.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class RescheduleJobRequest
{
	@Deprecated(forRemoval = true)
	@Schema(description = "Наименование работы")
	private String jobName;

	@NotNull
	@Schema(description = "Тип триггера (CRON/INTERVAL)", requiredMode = Schema.RequiredMode.REQUIRED)
	private JobTriggerType triggerType;

	@NotBlank
	@Schema(description = "Выражение для триггера", requiredMode = Schema.RequiredMode.REQUIRED)
	private String expression;
}
