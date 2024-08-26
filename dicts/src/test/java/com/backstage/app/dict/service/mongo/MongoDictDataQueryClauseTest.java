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

package com.backstage.app.dict.service.mongo;

import com.backstage.app.dict.common.CommonTest;
import com.backstage.app.dict.constant.ServiceFieldConstants;
import com.backstage.app.dict.service.backend.mongo.clause.MongoDictDataQueryClause;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MongoDictDataQueryClauseTest extends CommonTest
{
	@Autowired
	private MongoDictDataQueryClause mongoDictDataQueryClause;

	@Test
	void buildSortWithMultiplyOrders()
	{
		var requestOrders = List.of(Sort.Order.desc("integerField"), Sort.Order.desc("doubleField"));

		var sort = mongoDictDataQueryClause.buildSort(Sort.by(requestOrders), Set.of());

		var orders = sort.get()
				.collect(Collectors.toSet());

		var expectedOrders = new ArrayList<>(requestOrders);
		expectedOrders.add(Sort.Order.asc(ServiceFieldConstants._ID));

		assertEquals(orders.size(), 3);
		assertTrue(orders.containsAll(expectedOrders));
	}

	@Test
	void buildSortWithIdDescendingOrders()
	{
		var requestOrders = List.of(Sort.Order.desc(ServiceFieldConstants._ID));

		var sort = mongoDictDataQueryClause.buildSort(Sort.by(requestOrders), Set.of());

		var orders = sort.get()
				.collect(Collectors.toSet());

		assertEquals(orders.size(), 1);
		assertTrue(orders.containsAll(requestOrders));
	}

	@Test
	void buildSortWithoutOrders()
	{
		var sort = mongoDictDataQueryClause.buildSort(Sort.unsorted(), Set.of());

		var orders = sort.get()
				.collect(Collectors.toSet());

		assertEquals(orders.size(), 1);
		assertTrue(orders.contains(Sort.Order.asc(ServiceFieldConstants._ID)));
	}
}
