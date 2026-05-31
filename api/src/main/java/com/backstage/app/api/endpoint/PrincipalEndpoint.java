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

import com.backstage.app.api.conversion.dto.PrincipalConverter;
import com.backstage.app.api.model.ApiResponse;
import com.backstage.app.api.model.dto.PrincipalDto;
import com.backstage.app.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "principal-endpoint", description = "Методы для работы с авторизованным пользователем.")
@RestController
@RequestMapping("/api/principal")
@RequiredArgsConstructor
public class PrincipalEndpoint
{
	private final PrincipalConverter principalConverter;

	@GetMapping("/me")
	@Operation(summary = "Возвращает информацию о текущем пользователе.")
	public ApiResponse<PrincipalDto> getMe()
	{
		return ApiResponse.of(principalConverter.convert(SecurityUtils.getCurrentUser()));
	}
}
