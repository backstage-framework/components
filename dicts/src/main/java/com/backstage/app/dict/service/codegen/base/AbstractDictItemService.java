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

import com.backstage.app.dict.domain.DictItem;
import com.backstage.app.dict.model.dictitem.DictDataItem;
import com.backstage.app.dict.service.DictDataService;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public abstract class AbstractDictItemService<T extends AbstractDictItem>
{
	private final DictDataService dictDataService;

	public T getById(String itemId)
	{
		return buildItem(dictDataService.getById(getDictId(), itemId));
	}

	public List<T> getByIds(List<String> itemIds)
	{
		return dictDataService.getByIds(getDictId(), itemIds).stream()
				.map(this::buildItem)
				.collect(Collectors.toList());
	}

	public List<T> getByFilter(Condition condition, Pageable pageable)
	{
		return dictDataService.getByFilter(getDictId(), List.of(), ConditionBuilder.buildQuery(condition), pageable).stream()
				.map(this::buildItem)
				.collect(Collectors.toList());
	}

	public boolean existsById(String itemId)
	{
		return dictDataService.existsById(getDictId(), itemId);
	}

	public boolean existsByFilter(Condition condition)
	{
		return dictDataService.existsByFilter(getDictId(), ConditionBuilder.buildQuery(condition));
	}

	public T create(T item)
	{
		return buildItem(dictDataService.create(DictDataItem.of(getDictId(), item.toMap())));
	}

	public T update(T item)
	{
		return buildItem(dictDataService.update(item.getId(), DictDataItem.of(getDictId(), item.toMap()), item.getVersion()));
	}

	public void delete(String itemId)
	{
		delete(getById(itemId));
	}

	public void delete(T item)
	{
		delete(item, null);
	}

	public void delete(T item, String reason)
	{
		dictDataService.delete(item.getId(), reason, true, item.getVersion());
	}

	protected abstract String getDictId();

	protected abstract T buildItem(DictItem dictItem);
}
