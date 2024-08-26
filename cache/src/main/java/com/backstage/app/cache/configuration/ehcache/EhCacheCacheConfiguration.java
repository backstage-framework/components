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

package com.backstage.app.cache.configuration.ehcache;

import com.backstage.app.cache.configuration.CacheDecorator;
import com.backstage.app.cache.configuration.CacheSettings;
import com.backstage.app.cache.configuration.CacheSettingsProvider;
import com.backstage.app.cache.configuration.conditional.ConditionalOnCache;
import com.backstage.app.cache.configuration.jcache.EnhancedJCacheCacheManager;
import com.backstage.app.cache.configuration.properties.CacheProperties;
import lombok.RequiredArgsConstructor;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.jsr107.Eh107Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.cache.jcache.JCacheManagerFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Configuration
@ConditionalOnCache
@ConditionalOnClass(name = "org.ehcache.CacheManager")
@RequiredArgsConstructor
public class EhCacheCacheConfiguration
{
	@Bean
	public CacheManager cacheManager(Optional<CacheDecorator> cacheDecorator,
	                                 List<CacheSettings> cacheSettings,
	                                 JCacheManagerFactoryBean cacheManagerFactory,
	                                 CacheProperties cacheProperties)
	{
		var jCacheManager = cacheManagerFactory.getObject();

		if (jCacheManager == null)
		{
			throw new RuntimeException("JCacheManager is null");
		}

		cacheSettings.forEach(item -> {
					var config = CacheConfigurationBuilder.newCacheConfigurationBuilder(
									Object.class, Object.class, ResourcePoolsBuilder.heap(item.getMaxEntriesLocalHeap()))
							.withExpiry(ExpiryPolicyBuilder.expiry()
									.create(secondsToDuration(item.getTimeToLiveSeconds()))
									.access(secondsToDuration(item.getTimeToIdleSeconds()))
									.update(secondsToDuration(item.getTimeToLiveSeconds()))
									.build()
							);

					jCacheManager.createCache(item.getName(), Eh107Configuration.fromEhcacheCacheConfiguration(config));
				}
		);

		var cacheManager = new EnhancedJCacheCacheManager();
		cacheManager.setCacheDecorator(cacheDecorator.orElse(null));
		cacheManager.setTransactionAware(cacheProperties.isTransactional());
		cacheManager.setCacheManager(cacheManagerFactory.getObject());

		return cacheManager;
	}

	@Bean
	public CacheSettingsProvider ehCacheSettingsProvider()
	{
		return new CacheSettingsProvider(EhCacheConfigParser.parse());
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

	private Duration secondsToDuration(int seconds)
	{
		return Duration.ofSeconds(seconds == 0 ? Long.MAX_VALUE : seconds);
	}
}
