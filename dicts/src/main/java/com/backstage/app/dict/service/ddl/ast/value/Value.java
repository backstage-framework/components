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

package com.backstage.app.dict.service.ddl.ast.value;

import java.util.function.Function;

public abstract class Value<S>
{
	public abstract S getValue();

	public String asString()
	{
		return castValue(String.class, v -> v);
	}

	public Integer asInt()
	{
		return castValue(Number.class, Number::intValue);
	}

	private <T, R> R castValue(Class<T> clazz, Function<T, R> cast)
	{
		S value = getValue();

		if (value == null)
		{
			return null;
		}

		if (clazz.isInstance(value))
		{
			return cast.apply(clazz.cast(value));
		}
		else
		{
			throw new ClassCastException("Cannot cast '%s' to %s.".formatted(value, clazz));
		}
	}
}
