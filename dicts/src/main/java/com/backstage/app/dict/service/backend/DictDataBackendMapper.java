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

package com.backstage.app.dict.service.backend;

import com.backstage.app.dict.domain.DictItem;
import org.apache.commons.collections4.BidiMap;

import java.util.List;

public interface DictDataBackendMapper<T>
{
	default T mapTo(String dictId, DictItem dictItem)
	{
		return null;
	}

	default DictItem mapFromUsingAliases(String dictId, T source, BidiMap<String, String> dictAliasesRelation)
	{
		return new DictItem();
	}

	default DictItem mapFrom(String dictId, T source)
	{
		return new DictItem();
	}

	default List<DictItem> mapFrom(String dictId, List<T> source)
	{
		return source.stream()
				.map(it -> mapFrom(dictId, it))
				.toList();
	}

	default List<DictItem> mapFromUsingAliases(String dictId, List<T> source, BidiMap<String, String> dictAliasesRelation)
	{
		return source.stream()
				.map(it -> mapFromUsingAliases(dictId, it, dictAliasesRelation))
				.toList();
	}
}
