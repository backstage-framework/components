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

package com.backstage.app.dict.exception.dictitem;

import com.backstage.app.dict.exception.dict.DictException;

public class DictItemDeleteException extends DictException
{
	public DictItemDeleteException(String dictId, Throwable throwable)
	{
		super("При удалении всех элементов справочника '%s' произошла ошибка.".formatted(dictId), throwable);
	}

	public DictItemDeleteException(String dictId, String dictItemId, Throwable throwable)
	{
		super("При удалении элементы '%s' справочника '%s' произошла ошибка.".formatted(dictItemId, dictId), throwable);
	}
}
