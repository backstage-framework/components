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

import lombok.experimental.UtilityClass;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultDSLContext;

@UtilityClass
public class ConditionBuilder
{
	public DSLContext DSL_CONTEXT = new DefaultDSLContext(SQLDialect.POSTGRES);

	public Field<Object> field(String name)
	{
		return DSL.field(name);
	}

	public String buildQuery(Condition condition)
	{
		return DSL_CONTEXT.renderInlined(condition);
	}
}
