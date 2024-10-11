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

package com.backstage.app.dict.service.imp;

import com.backstage.app.dict.api.model.dto.ExportedDictDto;
import com.backstage.app.dict.api.model.dto.data.DictItemDto;
import com.backstage.app.dict.constant.ServiceFieldConstants;
import com.backstage.app.dict.domain.DictItem;
import com.backstage.app.dict.model.dictitem.DictDataItem;
import com.backstage.app.dict.service.DictDataService;
import com.backstage.app.dict.service.DictPermissionService;
import com.backstage.app.dict.service.DictService;
import com.backstage.app.utils.SecurityUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportJsonService implements ImportService
{
	private final JsonReader jsonReader;

	private final DictService dictService;
	private final DictDataService dictDataService;
	private final DictPermissionService dictPermissionService;

	public List<DictItem> importDict(String dictId, InputStream inputStream)
	{
		return importDict(dictId, inputStream, SecurityUtils.getCurrentUserId());
	}

	public List<DictItem> importDict(String dictId, InputStream inputStream, String userId)
	{
		var dict = dictService.getById(dictId);

		dictPermissionService.checkEditPermission(dict, userId);

		ExportedDictDto exportedDict = jsonReader.readFromStream(inputStream, new TypeReference<>() { });

		return exportedDict.getItems().stream()
				.map(item -> mapItem(dictId, item))
				.map(dictDataService::create)
				.collect(Collectors.toList());
	}

	private DictDataItem mapItem(String dictId, DictItemDto item)
	{
		var result = new HashMap<String, Object>();

		result.put(ServiceFieldConstants.ID, item.getId());
		result.put(ServiceFieldConstants.CREATED, item.getCreated());
		result.put(ServiceFieldConstants.UPDATED, item.getUpdated());

		result.putAll(item.getData());

		return DictDataItem.of(dictId, result);
	}
}
