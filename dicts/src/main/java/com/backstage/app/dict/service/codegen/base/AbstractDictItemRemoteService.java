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

import com.backstage.app.api.utils.RemoteServiceUtils;
import com.backstage.app.dict.api.dto.data.DictItemDto;
import com.backstage.app.dict.api.service.remote.InternalDictDataService;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public abstract class AbstractDictItemRemoteService<T extends AbstractDictItem>
{
	private final InternalDictDataService dictDataService;

	public List<T> getByIds(List<String> itemIds)
	{
		return RemoteServiceUtils.executeAndGetData(() -> dictDataService.getByIds(getDictId(), itemIds)).stream()
				.map(this::buildItem)
				.collect(Collectors.toList());
	}

	protected abstract String getDictId();

	protected abstract T buildItem(DictItemDto dictItem);
}
