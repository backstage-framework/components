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

package com.backstage.app.dict.api.endpoint;

import com.backstage.app.api.model.ApiResponse;
import com.backstage.app.dict.api.configuration.properties.DictsRemoteEndpointProperties;
import com.backstage.app.dict.api.dto.DictDto;
import com.backstage.app.dict.api.dto.DictEnumDto;
import com.backstage.app.dict.api.dto.request.CreateDictEnumRequest;
import com.backstage.app.dict.api.dto.request.CreateDictRequest;
import com.backstage.app.dict.api.dto.request.DeleteDictRequest;
import com.backstage.app.dict.api.service.remote.RemoteDictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dicts")
@Tag(name = "dict-endpoint", description = "Методы для работы со схемами справочников.")
@ConditionalOnProperty(DictsRemoteEndpointProperties.ACTIVATION_PROPERTY)
public class DictEndpoint implements RemoteDictService
{
	private final RemoteDictService remoteDictService;

	@Override
	@Operation(summary = "Получение схемы справочника по идентификатору.")
	@GetMapping("/get")
	public ApiResponse<DictDto> get(@RequestParam String id)
	{
		return remoteDictService.get(id);
	}

	@Override
	@Operation(summary = "Получение схем всех существующих справочников.")
	@GetMapping("/list")
	public ApiResponse<List<DictDto>> list()
	{
		return remoteDictService.list();
	}

	@Override
	@Operation(summary = "Добавление справочника.")
	@PostMapping("/create")
	public ApiResponse<DictDto> create(@RequestBody @Valid CreateDictRequest request)
	{
		return remoteDictService.create(request);
	}

	@Override
	@Operation(summary = "Добавление enum в справочник.")
	@PostMapping("/enum/create")
	public ApiResponse<DictEnumDto> createEnum(@RequestBody @Valid CreateDictEnumRequest request, @RequestParam String dictId)
	{
		return remoteDictService.createEnum(request, dictId);
	}

	@Override
	@Operation(summary = "Обновление enum в справочнике.")
	@PostMapping("/enum/update")
	public ApiResponse<DictEnumDto> updateEnum(@RequestBody @Valid CreateDictEnumRequest request, @RequestParam String dictId)
	{
		return remoteDictService.updateEnum(request, dictId);
	}

	@Override
	@Operation(summary = "Удаление enum в справочнике.")
	@PostMapping("/enum/delete")
	public ApiResponse<?> deleteEnum(@RequestParam String enumId, @RequestParam String dictId)
	{
		return remoteDictService.deleteEnum(enumId, dictId);
	}

	@Override
	@Operation(summary = "Обновление справочника.")
	@PostMapping("/update")
	public ApiResponse<DictDto> update(@RequestBody @Valid CreateDictRequest request)
	{
		return remoteDictService.update(request);
	}

	@Override
	@Operation(summary = "Удаление справочника (soft delete).")
	@PostMapping("/delete")
	public ApiResponse<?> delete(@RequestBody @Valid DeleteDictRequest request)
	{
		return remoteDictService.delete(request);
	}
}
