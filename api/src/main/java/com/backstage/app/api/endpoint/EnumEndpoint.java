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

package com.backstage.app.api.endpoint;

import com.backstage.app.api.model.ApiResponse;
import com.backstage.app.service.enums.ApiEnumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

@Tag(name = "enum-endpoint", description = "Методы для получения существующих enum'ов и их описаний.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/enum")
public class EnumEndpoint
{
	private final ApiEnumService apiEnumService;

	@Operation(summary = "Получение списка существующих enum'ов.")
	@GetMapping("/names")
	public ApiResponse<Set<String>> getEnumNames()
	{
		return ApiResponse.of(apiEnumService.getEnumNames());
	}

	@Operation(summary = "Получение описания enum'a.")
	@GetMapping("/description")
	public ApiResponse<Map<String, String>> getDescription(@Parameter(description = "Наименование enum'а") @RequestParam String name)
	{
		return ApiResponse.of(apiEnumService.getEnumDescription(name));
	}
}
