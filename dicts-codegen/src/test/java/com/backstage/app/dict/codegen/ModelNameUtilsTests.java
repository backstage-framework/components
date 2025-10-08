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

package com.backstage.app.dict.codegen;

import com.backstage.app.dict.utils.DictModelNameUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ModelNameUtilsTests
{
	@Test
	void testClassNames()
	{
		assertEquals("New", DictModelNameUtils.className("new"));
		assertEquals("New", DictModelNameUtils.className("NEW"));
		assertEquals("New", DictModelNameUtils.className("New"));

		assertEquals("NewClass", DictModelNameUtils.className("NewClass"));
		assertEquals("NewClass", DictModelNameUtils.className("newClass"));
		assertEquals("NewClass", DictModelNameUtils.className("new_Class"));
		assertEquals("NewClass", DictModelNameUtils.className("NEW_CLASS"));
	}

	@Test
	void testFieldNames()
	{
		assertEquals("new", DictModelNameUtils.fieldName("new"));
		assertEquals("new", DictModelNameUtils.fieldName("NEW"));
		assertEquals("new", DictModelNameUtils.fieldName("New"));

		assertEquals("newField", DictModelNameUtils.fieldName("NewField"));
		assertEquals("newField", DictModelNameUtils.fieldName("newField"));
		assertEquals("newField", DictModelNameUtils.fieldName("new_field"));
		assertEquals("newField", DictModelNameUtils.fieldName("new_Field"));
		assertEquals("newField", DictModelNameUtils.fieldName("NEW_FIELD"));
	}

	@Test
	void testEnumNames()
	{
		assertEquals("NEW", DictModelNameUtils.enumConstantName("new"));
		assertEquals("NEW", DictModelNameUtils.enumConstantName("NEW"));
		assertEquals("NEW", DictModelNameUtils.enumConstantName("New"));

		assertEquals("NEW_VALUE", DictModelNameUtils.enumConstantName("NewValue"));
		assertEquals("NEW_VALUE", DictModelNameUtils.enumConstantName("newValue"));
		assertEquals("NEW_VALUE", DictModelNameUtils.enumConstantName("NEW_VALUE"));
		assertEquals("NEW_VALUE", DictModelNameUtils.enumConstantName("new_value"));
		assertEquals("NEW_VALUE", DictModelNameUtils.enumConstantName("New_Value"));
	}
}
