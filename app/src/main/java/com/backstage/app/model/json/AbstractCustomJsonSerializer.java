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

package com.backstage.app.model.json;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonToken;
import tools.jackson.core.type.WritableTypeId;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.util.Collection;

/**
 * Компонент сериализует параметр с указанием наименования типа каждого элемента структуры.
 * Например: {"id":["java.lang.String", "asd"], "collection": ["java.util.ArrayList", ["java.lang.Integer", 1], ["java.lang.Integer", 2]]}.
 * Наследнику необходимо реализовать {@link AbstractCustomJsonSerializer#serializeValue(Object, JsonGenerator)} для осуществления логики сериализации параметра.
 * См. десериализацию {@link AbstractCustomJsonDeserializer}
 *
 * @param <T> - сериализуемый обьект.
 */
public abstract class AbstractCustomJsonSerializer<T> extends ValueSerializer<T>
{
	@Override
	public void serialize(T value, JsonGenerator gen, SerializationContext ctxt)
	{
		gen.writeStartObject();

		serializeValue(value, gen);

		gen.writeEndObject();
	}

	public abstract void serializeValue(T value, JsonGenerator gen);

	/**
	 * Сереализует значение в json ноду формата: "field": ["java.lang.ValueType", "value"].
	 *
	 * @param fieldName - наименование поля.
	 * @param value     - значение.
	 */
	protected void writeNodeWithTypePrefix(JsonGenerator gen, String fieldName, Object value)
	{
		if (value instanceof Collection<?> collection)
		{
			writeMultiValue(gen, fieldName, collection);

			return;
		}

		writeSingleValue(gen, fieldName, value);
	}

	protected void writeMultiValue(JsonGenerator gen, String fieldName, Collection<?> collection)
	{
		gen.writeArrayPropertyStart(fieldName);
		gen.writeTypePrefix(typePrefix(collection));

		collection.forEach(it -> writeValueWithTypePrefix(gen, it));

		gen.writeEndArray();
		gen.writeEndArray();
	}

	protected void writeSingleValue(JsonGenerator gen, String fieldName, Object value)
	{
		gen.writeName(fieldName);

		writeValueWithTypePrefix(gen, value);
	}

	private void writeValueWithTypePrefix(JsonGenerator gen, Object value)
	{
		if (value == null)
		{
			gen.writeNull();

			return;
		}

		gen.writeTypePrefix(typePrefix(value));
		gen.writePOJO(value);
		gen.writeEndArray();
	}

	private WritableTypeId typePrefix(Object value)
	{
		var typePrefix = new WritableTypeId(value, JsonToken.NOT_AVAILABLE);
		typePrefix.include = WritableTypeId.Inclusion.WRAPPER_ARRAY;
		typePrefix.id = value.getClass().getName();

		return typePrefix;
	}
}
