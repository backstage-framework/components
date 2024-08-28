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
import com.backstage.app.api.model.OkResponse;
import com.backstage.app.dict.api.configuration.properties.DictsRemoteEndpointProperties;
import com.backstage.app.dict.api.model.dto.DictDto;
import com.backstage.app.dict.api.model.dto.data.DictItemDto;
import com.backstage.app.dict.api.model.dto.data.request.CreateDictItemRequest;
import com.backstage.app.dict.api.model.dto.data.request.DeleteDictItemRequest;
import com.backstage.app.dict.api.model.dto.data.request.UpdateDictItemRequest;
import com.backstage.app.dict.api.model.dto.request.BasicSearchRequest;
import com.backstage.app.dict.api.model.dto.request.ExportDictRequest;
import com.backstage.app.dict.api.model.dto.request.SearchRequest;
import com.backstage.app.dict.api.service.remote.ExternalDictDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dicts")
@Tag(name = "dict-data-endpoint", description = "Методы для работы с данными справочников")
@ConditionalOnProperty(DictsRemoteEndpointProperties.ACTIVATION_PROPERTY)
public class DictDataEndpoint implements ExternalDictDataService
{
	private final ExternalDictDataService externalDictDataService;

	@Override
	@Operation(summary = "Получение схемы справочника по id.")
	@GetMapping("/{dictId}/scheme")
	public ApiResponse<DictDto> scheme(@Parameter(description = "Идентификатор справочника") @PathVariable String dictId)
	{
		return externalDictDataService.scheme(dictId);
	}

	@Override
	@Operation(summary = "Получение записей справочника по фильтру.")
	@PostMapping("/{dictId}/list")
	public ApiResponse<List<DictItemDto>> getByFilter(@PathVariable String dictId,
	                                                  @RequestBody @Valid SearchRequest request,
	                                                  @PageableDefault Pageable pageable)
	{
		return externalDictDataService.getByFilter(dictId, request, pageable);
	}

	@Override
	@Schema(description = "Получение списка ИД справочника по фильтру без пагинации.")
	@PostMapping("/{dictId}/ids")
	public ApiResponse<List<String>> getIdsByFilter(@Parameter(description = "Идентификатор справочника") @PathVariable String dictId,
	                                                @RequestBody BasicSearchRequest request)
	{
		return externalDictDataService.getIdsByFilter(dictId, request);
	}

	@Override
	@Operation(summary = "Получение записей справочника по списку id.")
	@PostMapping("/{dictId}/byIds")
	public ApiResponse<List<DictItemDto>> getByIds(@Parameter(description = "Идентификатор справочника") @PathVariable String dictId,
	                                                     @RequestBody List<String> ids)
	{
		return externalDictDataService.getByIds(dictId, ids);
	}

	@Override
	@Operation(summary = "Добавление записи в справочник.")
	@PostMapping("/{dictId}/create")
	public ApiResponse<DictItemDto> create(@Parameter(description = "Идентификатор справочника") @PathVariable String dictId,
	                                             @RequestBody @Valid CreateDictItemRequest request)
	{
		return externalDictDataService.create(dictId, request);
	}

	@Override
	@Operation(summary = "Обновление записи в справочнике.")
	@PostMapping("/{dictId}/update")
	public ApiResponse<DictItemDto> update(@Parameter(description = "Идентификатор справочника") @PathVariable String dictId,
	                                             @RequestBody @Valid UpdateDictItemRequest request)
	{
		return externalDictDataService.update(dictId, request);
	}

	@Override
	@Operation(summary = "Удаление записи в справочнике (soft delete).")
	@PostMapping("/{dictId}/delete")
	public ApiResponse<?> delete(@Parameter(description = "Идентификатор справочника") @PathVariable String dictId,
	                             @RequestBody @Valid DeleteDictItemRequest request)
	{
		return externalDictDataService.delete(dictId, request);
	}

	@Override
	@Schema(description = "Проверка существования записи в справочнике по идентификатору.")
	@PostMapping("/{dictId}/existsById")
	public ApiResponse<Boolean> existsById(@Parameter(description = "Идентификатор справочника") @PathVariable String dictId,
	                                       @Parameter(description = "Идентификатор записи справочника") @RequestParam String itemId)
	{
		return externalDictDataService.existsById(dictId, itemId);
	}

	@Override
	@Schema(description = "Проверка существования записи в справочнике по фильтру.")
	@PostMapping("/{dictId}/existsByFilter")
	public ApiResponse<Boolean> existsByFilter(@Parameter(description = "Идентификатор справочника") @PathVariable String dictId,
	                                           @RequestParam String filter)
	{
		return externalDictDataService.existsByFilter(dictId, filter);
	}

	@Override
	@Schema(description = "Получение количества записей в справочнике по фильтру.")
	@PostMapping("/{dictId}/countByFilter")
	public ApiResponse<Long> countByFilter(@Parameter(description = "Идентификатор справочника") @PathVariable String dictId,
	                                       @RequestBody BasicSearchRequest request)
	{
		return externalDictDataService.countByFilter(dictId, request);
	}

	@Override
	@Operation(summary = "Импорт CSV в справочник.")
	@PostMapping(value = "/{dictId}/import", consumes = "text/csv")
	public OkResponse importCsv(@Parameter(description = "Идентификатор справочника") @PathVariable String dictId,
	                            @RequestBody InputStream inputStream)
	{
		return externalDictDataService.importCsv(dictId, inputStream);
	}

	@Override
	@Operation(summary = "Импорт JSON в справочник.")
	@PostMapping(value = "/{dictId}/import", consumes = MediaType.APPLICATION_JSON_VALUE)
	public OkResponse importJson(@Parameter(description = "Идентификатор справочника") @PathVariable String dictId,
	                             @RequestBody InputStream inputStream)
	{
		return externalDictDataService.importJson(dictId, inputStream);
	}

	@Override
	@Operation(summary = "Экспорт элементов справочника.")
	@PostMapping("/{dictId}/export")
	public ResponseEntity<Resource> export(@Parameter(description = "Идентификатор справочника") @PathVariable String dictId,
	                                       @RequestBody @Valid ExportDictRequest request)
	{
		return externalDictDataService.export(dictId, request);
	}
}
