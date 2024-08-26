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

package com.backstage.app.conversion;

import com.backstage.app.common.AbstractTests;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
public class AbstractConverterTest extends AbstractTests
{
	private static final int PAGE_NUMBER = 1;
	private static final int PAGE_SIZE = 10;
	private static final int TOTAL_ELEMENTS = 7;

	@Autowired
	private NumberConverter numberConverter;

	@Test
	public void convertEmptyPage()
	{
		var page = numberConverter.convert(new PageImpl<>(Collections.emptyList(), PageRequest.of(PAGE_NUMBER, PAGE_SIZE), TOTAL_ELEMENTS));

		assertEquals(page.getPageable().getPageNumber(), PAGE_NUMBER);
		assertEquals(page.getPageable().getPageSize(), PAGE_SIZE);
		assertEquals(page.getTotalElements(), TOTAL_ELEMENTS);
	}

	@Test
	public void convertEmptySlice()
	{
		var slice = numberConverter.convert(new SliceImpl<>(Collections.emptyList(), PageRequest.of(PAGE_NUMBER, PAGE_SIZE), false));

		assertEquals(slice.getPageable().getPageNumber(), PAGE_NUMBER);
		assertEquals(slice.getPageable().getPageSize(), PAGE_SIZE);
		assertFalse(slice.hasNext());
	}
}
