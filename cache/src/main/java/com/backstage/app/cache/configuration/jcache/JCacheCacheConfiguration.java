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

package com.backstage.app.cache.configuration.jcache;

import com.backstage.app.cache.configuration.CacheDecorator;
import com.backstage.app.cache.configuration.CacheSettings;
import com.backstage.app.cache.configuration.conditional.ConditionalOnCache;
import com.backstage.app.cache.configuration.properties.CacheProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.cache.jcache.JCacheManagerFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Optional;

@Slf4j
@Configuration
@ConditionalOnCache
@RequiredArgsConstructor
public class JCacheCacheConfiguration
{
	@Bean
	public CacheManager cacheManager(Optional<CacheDecorator> cacheDecorator,
	                                 List<CacheSettings> cacheSettings,
									 JCacheSettingsAdapter jCacheSettingsAdapter,
	                                 JCacheManagerFactoryBean cacheManagerFactory,
	                                 CacheProperties cacheProperties)
	{
		var jCacheManager = cacheManagerFactory.getObject();

		if (jCacheManager == null)
		{
			throw new RuntimeException("JCacheManager is null");
		}

		cacheSettings.forEach(item -> {
			if (jCacheManager.getCache(item.getName()) == null)
			{
				jCacheManager.createCache(item.getName(), jCacheSettingsAdapter.convert(item));
			}
			else
			{
				log.warn("Trying to initialize cache '{}' twice.", item.getName());
			}
		});

		var cacheManager = new EnhancedJCacheCacheManager();
		cacheManager.setCacheDecorator(cacheDecorator.orElse(null));
		cacheManager.setTransactionAware(cacheProperties.isTransactional());
		cacheManager.setCacheManager(cacheManagerFactory.getObject());

		return cacheManager;
	}

	@Bean
	public KeyGenerator keyGenerator()
	{
		return new SimpleKeyGenerator();
	}

	@Bean
	public JCacheManagerFactoryBean cacheManagerFactory()
	{
		return new JCacheManagerFactoryBean();
	}
}
