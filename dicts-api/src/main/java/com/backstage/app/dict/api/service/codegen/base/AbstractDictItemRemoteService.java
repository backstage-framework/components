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

package com.backstage.app.dict.api.service.codegen.base;

import com.backstage.app.api.utils.RemoteServiceUtils;
import com.backstage.app.dict.api.model.dto.data.DictItemDto;
import com.backstage.app.dict.api.model.dto.request.SearchRequest;
import com.backstage.app.dict.api.service.remote.InternalDictDataService;
import com.backstage.app.exception.ObjectNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public abstract class AbstractDictItemRemoteService<T extends AbstractDictItem>
{
	private final InternalDictDataService dictDataService;

	public T getById(String itemId)
	{
		var items = getByIds(List.of(itemId));

		if (items.isEmpty())
		{
			throw new ObjectNotFoundException(Object.class, itemId);
		}

		return getByIds(List.of(itemId)).get(0);
	}

	public List<T> getByIds(List<String> itemIds)
	{
		return RemoteServiceUtils.executeAndGetData(() -> dictDataService.getByIds(getDictId(), itemIds)).stream()
				.map(this::buildItem)
				.collect(Collectors.toList());
	}

	public List<T> getByFilter(String query, Pageable pageable)
	{
		var searchRequest = SearchRequest.builder()
				.query(query)
				.build();

		return RemoteServiceUtils.executeAndGetData(() -> dictDataService.getByFilter(getDictId(), searchRequest, pageable))
				.stream()
				.map(this::buildItem)
				.collect(Collectors.toList());
	}

	protected abstract String getDictId();

	protected abstract T buildItem(DictItemDto dictItem);
}
