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

package com.backstage.app.dict.api.service;

import com.backstage.app.api.model.ApiResponse;
import com.backstage.app.api.model.OkResponse;
import com.backstage.app.dict.api.model.dto.DictDto;
import com.backstage.app.dict.api.model.dto.data.DictItemDto;
import com.backstage.app.dict.api.model.dto.data.request.CreateDictItemRequest;
import com.backstage.app.dict.api.model.dto.data.request.DeleteDictItemRequest;
import com.backstage.app.dict.api.model.dto.data.request.UpdateDictItemRequest;
import com.backstage.app.dict.api.model.dto.request.BasicSearchRequest;
import com.backstage.app.dict.api.model.dto.request.ExportDictRequest;
import com.backstage.app.dict.api.model.dto.request.SearchRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.util.List;

@Tag(name = "dict-data-endpoint", description = "Методы для работы с данными справочников")
public interface GenericDictDataService<T extends DictItemDto>
{
	@Schema(description = "Получение схемы справочника по id.")
	@GetMapping("/{dictId}/scheme")
	ApiResponse<DictDto> scheme(@Parameter(description = "Идентификатор справочника") @PathVariable String dictId);

	@Schema(description = "Получение записей справочника по фильтру.")
	@PostMapping("/{dictId}/list")
	ApiResponse<List<T>> getByFilter(@PathVariable String dictId, @RequestBody SearchRequest request,
	                                                 @SpringQueryMap Pageable pageable);

	@Schema(description = "Получение списка ИД справочника по фильтру без пагинации.")
	@PostMapping("/{dictId}/ids")
	ApiResponse<List<String>> getIdsByFilter(@PathVariable String dictId, @RequestBody BasicSearchRequest request);

	@Schema(description = "Получение записей справочника по списку id.")
	@PostMapping("/{dictId}/byIds")
	ApiResponse<List<T>> getByIds(@PathVariable String dictId, @RequestBody List<String> ids);

	@Schema(description = "Добавление записи в справочник.")
	@PostMapping("/{dictId}/create")
	ApiResponse<T> create(@PathVariable String dictId, @RequestBody CreateDictItemRequest request);

	@Schema(description = "Обновление записи в справочнике.")
	@PostMapping("/{dictId}/update")
	ApiResponse<T> update(@PathVariable String dictId, @RequestBody UpdateDictItemRequest request);

	@Schema(description = "Удаление записи в справочнике (soft delete).")
	@PostMapping("/{dictId}/delete")
	ApiResponse<?> delete(@PathVariable String dictId, @RequestBody DeleteDictItemRequest request);

	@Schema(description = "Проверка существования записи в справочнике по идентификатору.")
	@PostMapping("/{dictId}/existsById")
	ApiResponse<Boolean> existsById(@PathVariable String dictId, @RequestParam String itemId);

	@Schema(description = "Проверка существования записи в справочнике по фильтру.")
	@PostMapping("/{dictId}/existsByFilter")
	ApiResponse<Boolean> existsByFilter(@PathVariable String dictId, @RequestParam String filter);

	@Schema(description = "Получение количества записей в справочнике по фильтру.")
	@PostMapping("/{dictId}/countByFilter")
	ApiResponse<Long> countByFilter(@PathVariable String dictId, @RequestBody BasicSearchRequest request);

	@Operation(summary = "Импорт CSV в справочник.")
	@PostMapping(value = "/{dictId}/import", consumes = "text/csv")
	OkResponse importCsv(@PathVariable String dictId, @RequestBody InputStream inputStream);

	@Operation(summary = "Импорт JSON в справочник.")
	@PostMapping(value = "/{dictId}/import", consumes = MediaType.APPLICATION_JSON_VALUE)
	OkResponse importJson(@PathVariable String dictId, @RequestBody InputStream inputStream);

	@Operation(summary = "Экспорт элементов справочника.")
	@PostMapping("/{dictId}/export")
	ResponseEntity<Resource> export(@PathVariable String dictId, @RequestBody @Valid ExportDictRequest request);
}
