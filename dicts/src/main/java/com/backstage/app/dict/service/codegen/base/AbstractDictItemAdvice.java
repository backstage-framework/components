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

import com.backstage.app.dict.domain.Dict;
import com.backstage.app.dict.domain.DictItem;
import com.backstage.app.dict.service.advice.DictDataServiceAdvice;
import com.google.common.base.Suppliers;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.function.Supplier;

public abstract class AbstractDictItemAdvice<T extends AbstractDictItem, S extends AbstractDictItemService<T>> implements DictDataServiceAdvice
{
	private final Supplier<S> dictItemServiceSupplier;

	public AbstractDictItemAdvice(ObjectProvider<S> dictItemServiceProvider)
	{
		this.dictItemServiceSupplier = Suppliers.memoize(dictItemServiceProvider::getObject);
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
		if (getDictItemService().getDictId().equals(dict.getId()))
		{
			var dictItem = getDictItemService().buildItem(item);

			handleBeforeCreate(dictItem);

			item.getData().clear();
			item.getData().putAll(dictItem.toMap());
		}
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
	}

	@Override
	public final void handleAfterCreateMany(Dict dict, List<DictItem> items)
	{
	}

	public void handleAfterCreateMany(List<T> items)
	{
	}

	@Override
	public final void handleUpdate(Dict dict, DictItem oldItem, DictItem dictItem)
	{
		if (getDictItemService().getDictId().equals(dict.getId()))
		{
			var item = getDictItemService().buildItem(dictItem);

			handleBeforeUpdate(
					getDictItemService().buildItem(oldItem),
					item
			);

			dictItem.getData().clear();
			dictItem.getData().putAll(item.toMap());
		}
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
	public final void handleDelete(Dict dict, DictItem item, boolean deleted)
	{
		if (getDictItemService().getDictId().equals(dict.getId()))
		{
			handleDelete(getDictItemService().buildItem(item), deleted);
		}
	}

	public void handleDelete(T item, boolean deleted)
	{
	}

	@Override
	public final void handleDeleteAll(Dict dict, boolean deleted)
	{
		if (getDictItemService().getDictId().equals(dict.getId()))
		{
			handleDeleteAll(deleted);
		}
	}

	public void handleDeleteAll(boolean deleted)
	{
	}

	protected S getDictItemService()
	{
		return dictItemServiceSupplier.get();
	}
}
