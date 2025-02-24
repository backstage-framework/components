/*
 *    Copyright 2019-2025 the original author or authors.
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

package com.backstage.app.dict.service.advice;

import com.backstage.app.dict.domain.Dict;
import com.backstage.app.dict.domain.DictConstraint;
import com.backstage.app.dict.domain.DictEnum;
import com.backstage.app.dict.domain.DictIndex;
import org.springframework.core.annotation.Order;

@Order(0)
public interface DictServiceAdvice
{
	default void handleGetById(String id)
	{
	}

	default void handleGetAll()
	{
	}

	default void handleExistsById(String id)
	{
	}

	default void handleBeforeCreate(Dict dict)
	{
	}

	default void handleAfterCreate(Dict dict)
	{
	}

	default void handleBeforeUpdate(Dict oldDict, Dict dict)
	{
	}

	default void handleAfterUpdate(Dict dict)
	{
	}

	default void handleDelete(Dict dict)
	{
	}

	default void handleRenameField(Dict dict, String fieldId, String newFieldId, String newFieldName)
	{
	}

	default void handleCreateConstraint(Dict dict, DictConstraint constraint)
	{
	}

	default void handleDeleteConstraint(Dict dict, String constraintId)
	{
	}

	default void handleCreateIndex(Dict dict, DictIndex index)
	{
	}

	default void handleDeleteIndex(Dict dict, String indexId)
	{
	}

	default void handleCreateEnum(Dict dict, DictEnum dictEnum)
	{
	}

	default void handleUpdateEnum(Dict dict, DictEnum dictEnum)
	{
	}

	default void handleDeleteEnum(Dict dict, String enumId)
	{
	}
}
