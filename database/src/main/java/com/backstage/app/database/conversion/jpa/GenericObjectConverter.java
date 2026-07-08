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

package com.backstage.app.database.conversion.jpa;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

public class GenericObjectConverter extends AbstractMutableJsonConverter<Object>
{
	protected TypeReference<Object> createTypeReference()
	{
		return new TypeReference<>() { };
	}

	@Override
	protected void createObjectMapper()
	{
		mapper = new ObjectMapper().rebuild()
				.activateDefaultTyping(BasicPolymorphicTypeValidator.builder().build(), DefaultTyping.NON_FINAL_AND_ENUMS)
				.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
				.build();
	}
}
