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

import com.backstage.app.dict.domain.DictItem;
import com.backstage.app.dict.exception.dict.DictException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface ImportService
{
	default List<DictItem> importDict(String dictId, String resourceName)
	{
		try (var inputStream = getClass().getResourceAsStream(resourceName))
		{
			if (inputStream == null)
			{
				throw new IOException("resource not found: " + resourceName);
			}

			return importDict(dictId, inputStream);
		}
		catch (Exception e)
		{
			throw new DictException("failed to import dict '%s'".formatted(dictId), e);
		}
	}

	List<DictItem> importDict(String dictId, InputStream inputStream);
}
