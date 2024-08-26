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

import com.backstage.app.cache.configuration.distributed.annotation.DistributedCacheEvict;
import com.backstage.app.configuration.properties.AppProperties;
import com.google.common.base.Suppliers;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.jms.annotation.JmsListener;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@Slf4j
@RequiredArgsConstructor
public class DistributedCacheListener implements ApplicationContextAware
{
	@Setter
	@Getter
	private ApplicationContext applicationContext;

	@Resource(name = "cacheManager")
	private CacheManager cacheManager;

	private final DistributedCacheService distributedCacheService;

	private final AppProperties appProperties;

	private String nodeId;

	private final Map<String, Pair<Supplier<Object>, Method>> cacheItemInvalidators = new HashMap<>();
	private final Map<String, Pair<Supplier<Object>, Method>> cacheInvalidators = new HashMap<>();

	@PostConstruct
	public void initialize()
	{
		nodeId = distributedCacheService.getNodeId();

		var scanner = new ClassPathScanningCandidateComponentProvider(false);
		scanner.addIncludeFilter((metadataReader, metadataReaderFactory) -> {
			try
			{
				var targetClass = Class.forName(metadataReader.getClassMetadata().getClassName());

				MethodUtils.getMethodsListWithAnnotation(targetClass, DistributedCacheEvict.class).forEach(method -> {
					var annotation = method.getAnnotation(DistributedCacheEvict.class);
					var cacheName = annotation.value();

					switch (method.getParameterCount())
					{
						case 0 -> cacheInvalidators.put(cacheName, ImmutablePair.of(Suppliers.memoize(() -> getApplicationContext().getBean(targetClass)), method));
						case 1 -> cacheItemInvalidators.put(cacheName, ImmutablePair.of(Suppliers.memoize(() -> getApplicationContext().getBean(targetClass)), method));

						default -> throw new RuntimeException("distributed cache evict method may only have one parameter");
					}

					log.info("Registering distributed cache invalidator for cache '{}'.", cacheName);
				});
			}
			catch (Throwable ignored)
			{
				// Игнорируем
			}

			return false;
		});

		appProperties.getBasePackages().forEach(scanner::findCandidateComponents);
	}

	@JmsListener(destination = DistributedCacheConfiguration.DISTRIBUTED_CACHE_ITEMS_TOPIC, containerFactory = "cacheListenerContainerFactory")
	public void onItemReceived(DistributedCacheItem item)
	{
		try
		{
			DistributedCacheItemSynchronizer.setCurrentItem(item);

			// Обрабатываем сообщения только от других узлов.
			if (!StringUtils.equals(item.getSourceNode(), nodeId))
			{
				log.debug("Processing distributed cache item: {}.", item);

				var cacheOperations = distributedCacheService.getCacheOperations().get(item.getCacheName());

				if (cacheOperations != null)
				{
					var targetCache = cacheManager.getCache(item.getCacheName());

					if (targetCache != null)
					{
						if (item.getKey() == null)
						{
							var cacheInvalidator = cacheInvalidators.get(item.getCacheName());

							if (cacheInvalidator != null)
							{
								invokeInvalidator(cacheInvalidator, item);
							}
							else
							{
								targetCache.invalidate();
							}
						}
						else
						{
							if (item.getValue() != null && DistributedCacheOperations.isPropagatingPut(cacheOperations))
							{
								log.debug("Applying new cache item: {}.", item);

								targetCache.put(item.getKey(), item.getValue());
							}
							else
							{
								log.debug("Clearing cache item: {}.", item);

								var cacheItemInvalidator = cacheItemInvalidators.get(item.getCacheName());

								if (cacheItemInvalidator != null)
								{
									invokeInvalidator(cacheItemInvalidator, item);
								}
								else
								{
									targetCache.evict(item.getKey());
								}
							}
						}
					}
				}
			}
		}
		finally
		{
			DistributedCacheItemSynchronizer.setCurrentItem(null);
		}
	}

	private void invokeInvalidator(Pair<Supplier<Object>, Method> invalidator, DistributedCacheItem item)
	{
		try
		{
			var method = invalidator.getValue();
			var instance = invalidator.getKey().get();

			if (item.getKey() == null)
			{
				method.invoke(instance);
			}
			else
			{
				method.invoke(instance, item.getKey());
			}
		}
		catch (Exception e)
		{
			log.error("Failed to execute distributed cache invalidator for cache '{}'.", item.getCacheName(), e);
		}
	}
}
