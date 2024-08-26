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

package com.backstage.app.cache.configuration.distributed;

import com.backstage.app.cache.configuration.CacheDecorator;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.Cache;
import org.springframework.jms.core.JmsTemplate;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class DistributedCacheService implements CacheDecorator
{
	@Resource(name = "cacheJmsTemplate")
	private JmsTemplate jmsTemplate;

	@Getter
	private final DistributedCacheOperations cacheOperations;

	@Getter
	private String nodeId;

	@PostConstruct
	void initialize()
	{
		nodeId = UUID.randomUUID().toString();

		log.info("Initializing distributed cache node '{}'.", nodeId);
	}

	public void invalidate(String cacheName)
	{
		evict(cacheName, null);
	}

	public void evict(String cacheName, Object key)
	{
		put(cacheName, key, null);
	}

	public void put(String cacheName, Object key, Object value)
	{
		if (value != null && !DistributedCacheOperations.isPropagatingPut(cacheOperations.get(cacheName)))
		{
			return;
		}

		send(new DistributedCacheItem(cacheName, key, value, nodeId));
	}

	public void send(DistributedCacheItem item)
	{
		var currentItem = DistributedCacheItemSynchronizer.getCurrentItem().get();

		// Исключаем возникновение бесконечной петли.
		if (currentItem == null || !StringUtils.equals(item.getCacheName(), currentItem.getCacheName()) || !Objects.equals(item.getKey(), currentItem.getKey()))
		{
			jmsTemplate.convertAndSend(DistributedCacheConfiguration.DISTRIBUTED_CACHE_ITEMS_TOPIC, item);
		}
	}

	@Override
	public Cache decorate(Cache cache)
	{
		return cacheOperations.contains(cache.getName())
				? new DistributedCacheDelegate(cache, this) : cache;
	}
}