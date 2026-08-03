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

package com.backstage.app.dict.service.codegen.server.base;

import com.backstage.app.dict.domain.Dict;
import com.backstage.app.dict.domain.DictItem;
import com.backstage.app.dict.exception.dict.DictException;
import com.backstage.app.dict.service.advice.DictDataServiceAdvice;
import com.backstage.app.dict.service.codegen.client.base.AbstractDictItem;
import com.backstage.app.utils.SpringContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.function.Supplier;

@Slf4j
public abstract class AbstractDictItemAdvice<T extends AbstractDictItem, S extends AbstractDictItemService<T>> implements DictDataServiceAdvice
{
	private final Supplier<S> dictItemServiceSupplier;

	public AbstractDictItemAdvice()
	{
		this.dictItemServiceSupplier = SpringContextUtils.createBeanSupplier(getDictItemServiceClass());
	}

	@Override
	public final void handleGetByIds(Dict dict, List<String> ids)
	{
	}

	@Override
	public final void handleGetByFilter(Dict dict, List<String> selectFields, String query, Pageable pageable)
	{
	}

	@Override
	public final void handleExistsById(Dict dict, String itemId)
	{
	}

	@Override
	public final void handleExistsByFilter(Dict dict, String query)
	{
	}

	@Override
	public final void handleCountByFilter(Dict dict, String query)
	{
	}

	@Override
	public final void handleBeforeCreate(Dict dict, DictItem item)
	{
		var dictItemService = getDictItemService();

		if (dictItemService.getDictId().equals(dict.getId()))
		{
			if (!dict.getVersion().equals(dictItemService.getDictVersion()))
			{
				handleBeforeCreateFallback(dict, item);
			}
			else
			{
				var dictItem = dictItemService.buildItem(item);

				handleBeforeCreate(dictItem);

				item.getData().clear();
				item.getData().putAll(dictItem.toMap());
			}
		}
	}

	public void handleBeforeCreateFallback(Dict dict, DictItem item)
	{
		handleDictVersionMismatch(dict);
	}

	public void handleBeforeCreate(T item)
	{
	}

	@Override
	public final void handleAfterCreate(Dict dict, DictItem item)
	{
		if (getDictItemService().getDictId().equals(dict.getId()))
		{
			handleAfterCreate(getDictItemService().buildItem(item));
		}
	}

	public void handleAfterCreate(T item)
	{
	}

	@Override
	public final void handleBeforeCreateMany(Dict dict, List<DictItem> items)
	{
		if (getDictItemService().getDictId().equals(dict.getId()))
		{
			items.forEach(item -> handleBeforeCreate(dict, item));
		}
	}

	@Override
	public final void handleAfterCreateMany(Dict dict, List<DictItem> items)
	{
		if (getDictItemService().getDictId().equals(dict.getId()))
		{
			items.forEach(item -> handleAfterCreate(dict, item));
		}
	}

	@Override
	public final void handleUpdate(Dict dict, DictItem oldItem, DictItem dictItem)
	{
		var dictItemService = getDictItemService();

		if (dictItemService.getDictId().equals(dict.getId()))
		{
			if (!dict.getVersion().equals(dictItemService.getDictVersion()))
			{
				handleBeforeUpdateFallback(dict, oldItem, dictItem);
			}
			else
			{
				var item = dictItemService.buildItem(dictItem);

				handleBeforeUpdate(dictItemService.buildItem(oldItem), item);

				dictItem.getData().clear();
				dictItem.getData().putAll(item.toMap());
			}
		}
	}

	public void handleBeforeUpdateFallback(Dict dict, DictItem oldItem, DictItem dictItem)
	{
		handleDictVersionMismatch(dict);
	}

	private void handleDictVersionMismatch(Dict dict)
	{
		log.error("Schema version mismatch detected for dict '{}'! Client was generated for version: {}, but actual dict version: {}. Please consider implementing a fallback method.",
				dict.getId(),
				getDictItemService().getDictVersion(),
				dict.getVersion());

		throw new DictException("dict schema mismatch detected in generated dict client");
	}

	public void handleBeforeUpdate(T oldItem, T item)
	{
	}

	@Override
	public final void handleAfterUpdate(Dict dict, DictItem item)
	{
		if (getDictItemService().getDictId().equals(dict.getId()))
		{
			handleAfterUpdate(getDictItemService().buildItem(item));
		}
	}

	public void handleAfterUpdate(T item)
	{
	}

	@Override
	public final void handleDelete(Dict dict, DictItem item)
	{
		if (getDictItemService().getDictId().equals(dict.getId()))
		{
			handleDelete(getDictItemService().buildItem(item));
		}
	}

	public void handleDelete(T item)
	{
	}

	@Override
	public final void handleDeleteAll(Dict dict)
	{
		if (getDictItemService().getDictId().equals(dict.getId()))
		{
			handleDeleteAll();
		}
	}

	public void handleDeleteAll()
	{
	}

	protected abstract Class<S> getDictItemServiceClass();

	protected S getDictItemService()
	{
		return dictItemServiceSupplier.get();
	}
}
