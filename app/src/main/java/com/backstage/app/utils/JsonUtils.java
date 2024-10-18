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

package com.backstage.app.utils;

import com.backstage.app.exception.AppException;
import com.backstage.app.model.other.exception.ApiStatusCodeImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Supplier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class JsonUtils
{
	private static final Supplier<ObjectMapper> mapperSupplier = SpringContextUtils.createBeanSupplier(ObjectMapper.class);

	public static String toJson(Object value)
	{
		try
		{
			return mapperSupplier.get().writeValueAsString(value);
		}
		catch (JsonProcessingException e)
		{
			throw new AppException(ApiStatusCodeImpl.SERIALIZE_ERROR,
					"При сериализации объекта '%s', произошла ошибка: %s".formatted(value, e));
		}
	}

	public static List<Map<String, Object>> toList(Object value)
	{
		try
		{
			return mapperSupplier.get().readValue(value.toString(), new TypeReference<>() { });
		}
		catch (JsonProcessingException e)
		{
			throw new AppException(ApiStatusCodeImpl.DESERIALIZE_ERROR,
					"При десериализации объекта '%s', произошла ошибка: %s".formatted(value, e));
		}
	}

	public static Map<String, Object> toObject(Object value)
	{
		return toObject(value, new TypeReference<>() { });
	}

	public static <T> T toObject(Object value, Class<T> clazz)
	{
		try
		{
			var mapper = mapperSupplier.get();

			return mapper.readValue(value.toString(), mapper.constructType(clazz));
		}
		catch (JsonProcessingException e)
		{
			throw new AppException(ApiStatusCodeImpl.DESERIALIZE_ERROR,
					"При десериализации объекта '%s', произошла ошибка: %s".formatted(value, e));
		}
	}

	public static <T> T toObject(Object value, TypeReference<T> typeReference)
	{
		try
		{
			return mapperSupplier.get().readValue(value.toString(), typeReference);
		}
		catch (JsonProcessingException e)
		{
			throw new AppException(ApiStatusCodeImpl.DESERIALIZE_ERROR,
					"При десериализации объекта '%s', произошла ошибка: %s".formatted(value, e));
		}
	}
}
