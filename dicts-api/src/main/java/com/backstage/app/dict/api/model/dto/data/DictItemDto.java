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

package com.backstage.app.dict.api.model.dto.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Schema(description = "Запись в справочнике")
public class DictItemDto
{
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String id;

	@Schema(description = "Пользовательские поля")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private Map<String, Object> data = new HashMap<>();

	@Schema(description = "История изменений записи")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List<Map<String, Object>> history = new ArrayList<>();

	@Schema(description = "Текущая версия записи")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private Long version;

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private LocalDateTime created;

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private LocalDateTime updated;

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private LocalDateTime deleted;

	@Schema(description = "Причина удаления")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String deletionReason;
}
