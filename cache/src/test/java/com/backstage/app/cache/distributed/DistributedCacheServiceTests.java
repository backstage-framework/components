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

package com.backstage.app.cache.distributed;

import com.backstage.app.cache.AbstractTests;
import com.backstage.app.cache.configuration.distributed.DistributedCacheItem;
import com.backstage.app.cache.configuration.distributed.DistributedCacheService;
import com.backstage.app.utils.TimeUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DistributedCacheServiceTests extends AbstractTests
{
	@Autowired private CacheManager cacheManager;

	@Autowired private DistributedCacheService distributedCacheService;
	@Autowired private SingleItemCacheInvalidator singleItemCacheInvalidator;
	@Autowired private AllItemsCacheInvalidator allItemsCacheInvalidator;

	@Test
	public void distributedSingleItemInvalidatorTest()
	{
		var cacheName = SingleItemCacheInvalidator.CACHE_NAME;
		var itemId = "1";
		var item = new Object();

		var cache = cacheManager.getCache(cacheName);

		assertNotNull(cache);

		cache.put(itemId, item);

		assertNotNull(cache.get(itemId));
		assertEquals(cache.get(itemId).get(), item);

		assertFalse(singleItemCacheInvalidator.isInvalidated());

		distributedCacheService.send(new DistributedCacheItem(cacheName, itemId, null, UUID.randomUUID().toString()));

		TimeUtils.sleep(100);

		assertTrue(singleItemCacheInvalidator.isInvalidated());

		assertNull(cache.get(itemId));
	}

	@Test
	public void distributedAllItemsInvalidatorTest()
	{
		var cacheName = AllItemsCacheInvalidator.CACHE_NAME;
		var itemIds = List.of("1", "2", "3");

		var cache = cacheManager.getCache(cacheName);

		assertNotNull(cache);

		itemIds.forEach(itemId -> cache.put(itemId, new Object()));
		itemIds.forEach(itemId -> assertNotNull(cache.get(itemId)));

		assertFalse(allItemsCacheInvalidator.isInvalidated());

		distributedCacheService.send(new DistributedCacheItem(cacheName, null, null, UUID.randomUUID().toString()));

		TimeUtils.sleep(100);

		assertTrue(allItemsCacheInvalidator.isInvalidated());

		itemIds.forEach(itemId -> assertNull(cache.get(itemId)));
	}
}
