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

package com.backstage.app.dict.service.codegen.server.generator;

import com.backstage.app.dict.domain.DictItem;
import com.backstage.app.dict.service.DictDataService;
import com.backstage.app.dict.service.codegen.server.base.AbstractDictItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Type;

@Slf4j
@RequiredArgsConstructor
public class DictItemServiceGenerator extends com.backstage.app.dict.service.codegen.client.generator.DictItemServiceGenerator
{
	protected Class<?> getBaseType()
	{
		return AbstractDictItemService.class;
	}

	protected Type getDictDataServiceType()
	{
		return DictDataService.class;
	}

	protected Type getDictItemSourceType()
	{
		return DictItem.class;
	}
}
