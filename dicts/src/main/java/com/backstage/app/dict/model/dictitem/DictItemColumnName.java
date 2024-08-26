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

package com.backstage.app.dict.model.dictitem;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

@AllArgsConstructor
@Getter
public enum DictItemColumnName
{
	ID("id"),
	DATA("data"),
	HISTORY("history"),
	VERSION("version"),
	CREATED("created"),
	UPDATED("updated"),
	DELETED("deleted"),
	// TODO для dictId/dictFieldId/...Id Postgres адаптера, реализовать маппинг camelCase в snake_case
	DELETION_REASON("deletionreason");

	private final String name;

	public static final Set<String> SERVICE_COLUMNS = Set.of(ID.getName(), HISTORY.getName(), VERSION.getName(),
			CREATED.getName(), UPDATED.getName(), DELETED.getName(), DELETION_REASON.getName());
}
