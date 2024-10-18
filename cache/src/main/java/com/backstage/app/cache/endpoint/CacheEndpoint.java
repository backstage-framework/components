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

package com.backstage.app.cache.endpoint;

import com.backstage.app.api.model.ApiResponse;
import com.backstage.app.api.model.OkResponse;
import com.backstage.app.cache.configuration.conditional.ConditionalOnCache;
import com.backstage.app.cache.service.CacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@Tag(name = "cache-endpoint", description = "Методы для работы с кэшированными в сервисе данными.")
@RestController
@RequestMapping("/api/cache")
@ConditionalOnCache
@RequiredArgsConstructor
public class CacheEndpoint
{
	private final CacheService cacheService;

	@GetMapping("/names")
	@Operation(summary = "Получение идентификаторов всех кэшей приложения.")
	public ApiResponse<Set<String>> names()
	{
		return ApiResponse.of(cacheService.getCacheNames());
	}

	@PostMapping("/invalidate")
	@Operation(summary = "Инвалидирует указанный кэш.")
	public OkResponse invalidate(@RequestParam String name)
	{
		cacheService.invalidate(name);

		return ApiResponse.ok();
	}

	@PostMapping("/invalidateAll")
	@Operation(summary = "Инвалидирует все кэши сервиса.")
	public OkResponse invalidate()
	{
		cacheService.invalidate();

		return ApiResponse.ok();
	}
}
