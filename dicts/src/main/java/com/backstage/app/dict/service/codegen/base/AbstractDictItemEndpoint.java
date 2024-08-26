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

package com.backstage.app.dict.service.codegen.base;

import com.backstage.app.api.model.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@RequiredArgsConstructor
public abstract class AbstractDictItemEndpoint<T extends AbstractDictItem, S extends AbstractDictItemService<T>>
{
	private final S dictItemService;

	@Operation(summary = "Получение записей справочника по списку id.")
	@PostMapping({"/byIds"})
	public ApiResponse<List<T>> getByIds(@RequestBody List<String> ids)
	{
		return ApiResponse.of(dictItemService.getByIds(ids));
	}
}
