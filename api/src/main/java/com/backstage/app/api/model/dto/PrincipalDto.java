/*
 *    Copyright 2019-2026 the original author or authors.
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

package com.backstage.app.api.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PrincipalDto
{
	@Schema(description = "Идентификатор пользователя")
	private String id;

	@Schema(description = "Набор разрешений")
	private List<String> permissions;

	@Schema(description = "Дополнительная информация о пользователе", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private Object details;
}
