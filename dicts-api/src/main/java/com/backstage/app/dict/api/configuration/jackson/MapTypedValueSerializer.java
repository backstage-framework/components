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

package com.backstage.app.dict.api.configuration.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.Collection;

public class MapTypedValueSerializer extends StdSerializer<Object>
{
	protected MapTypedValueSerializer()
	{
		super(Object.class);
	}

	@Override
	public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException
	{
		if (value instanceof Collection<?> collection)
		{
			if (!collection.isEmpty())
			{
				var collectionType = provider.getTypeFactory().constructCollectionType(collection.getClass(), collection.iterator().next().getClass());
				var serializer = provider.findTypedValueSerializer(collectionType, true, null);

				serializer.serialize(value, gen, provider);

				return;
			}
		}

		var serializer = provider.findTypedValueSerializer(value.getClass(), true, null);

		serializer.serialize(value, gen, provider);
	}
}
