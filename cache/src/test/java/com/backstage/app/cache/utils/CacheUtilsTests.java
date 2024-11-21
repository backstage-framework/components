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

package com.backstage.app.cache.utils;

import com.backstage.app.cache.AbstractTests;
import com.backstage.app.cache.utils.proxy.ForceProxy;
import com.backstage.app.cache.utils.proxy.NoProxy;
import com.backstage.app.cache.utils.proxy.ReadOnlyObjectProxyFactory;
import com.backstage.app.cache.utils.proxy.model.FieldProxySupplier;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class CacheUtilsTests extends AbstractTests
{
	@Component
	public static class MapFieldSupplier implements FieldProxySupplier<CacheItem>
	{
		public static final Map<String, String> TEST_MAP = Map.of(
				UUID.randomUUID().toString(), UUID.randomUUID().toString()
		);

		@Override
		public Object getProxy(CacheItem source, Field field)
		{
			return TEST_MAP;
		}
	}

	@Data
	@NoArgsConstructor
	public static class CacheItem
	{
		@Data
		@NoArgsConstructor
		@AllArgsConstructor
		public static class InnerItem
		{
			private String id;
		}

		private String id;

		@NoProxy
		private String ignoredId;

		private List<String> ignoredList;

		@ForceProxy
		private List<String> basicList;

		@ForceProxy
		private Set<UUID> uuidSet;

		@ForceProxy
		private List<InnerItem> complexList;

		private Map<String, String> ignoredMap;

		@ForceProxy
		private Map<String, String> basicMap;

		@ForceProxy
		private Map<String, InnerItem> complexMap;

		@ForceProxy(proxySupplierBean = MapFieldSupplier.class)
		private Map<String, String> suppliedMap;

		public CacheItem(UUID id)
		{
			var stringId = id.toString();

			this.id = stringId;
			this.ignoredId = stringId;

			this.ignoredList = List.of(stringId);
			this.basicList = List.of(stringId);
			this.uuidSet = Set.of(id);
			this.complexList = List.of(new InnerItem(stringId));

			this.ignoredMap = Map.of(stringId, stringId);
			this.basicMap = Map.of(stringId, stringId);
			this.complexMap = Map.of(stringId, new InnerItem(stringId));
		}
	}

	@Test
	public void checkReadOnlyProxy()
	{
		// TODO: проверки для @Entity
		var sourceItem = new CacheItem(UUID.randomUUID());
		var cachedItem = ReadOnlyObjectProxyFactory.createProxy(sourceItem, CacheItem.class);

		Assertions.assertEquals(sourceItem.getId(), cachedItem.getId());
		assertNotNull(sourceItem.getIgnoredId());
		assertNull(cachedItem.getIgnoredId());
		assertNull(sourceItem.getSuppliedMap());

		assertNotNull(sourceItem.getIgnoredList());
		assertNull(cachedItem.getIgnoredList());
		assertIterableEquals(sourceItem.getBasicList(), cachedItem.getBasicList());
		assertIterableEquals(sourceItem.getUuidSet(), cachedItem.getUuidSet());
		assertIterableEquals(sourceItem.getComplexList(), cachedItem.getComplexList());

		assertNotNull(sourceItem.getIgnoredMap());
		assertNull(cachedItem.getIgnoredMap());
		assertIterableEquals(sourceItem.getBasicMap().entrySet(), cachedItem.getBasicMap().entrySet());
		assertIterableEquals(sourceItem.getComplexMap().entrySet(), cachedItem.getComplexMap().entrySet());
		assertIterableEquals(cachedItem.getSuppliedMap().entrySet(), MapFieldSupplier.TEST_MAP.entrySet());
	}
}
