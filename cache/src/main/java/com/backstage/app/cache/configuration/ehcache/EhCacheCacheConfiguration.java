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

package com.backstage.app.cache.configuration.ehcache;

import com.backstage.app.cache.configuration.CacheSettingsProvider;
import com.backstage.app.cache.configuration.conditional.ConditionalOnCache;
import com.backstage.app.cache.configuration.jcache.JCacheSettingsAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.jsr107.Eh107Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Slf4j
@Configuration
@ConditionalOnCache
@ConditionalOnClass(name = "org.ehcache.CacheManager")
@RequiredArgsConstructor
public class EhCacheCacheConfiguration
{
	@Bean
	public JCacheSettingsAdapter ehCacheJCacheSettingsAdapter()
	{
		return settings -> {
			var config = CacheConfigurationBuilder.newCacheConfigurationBuilder(
							Object.class, Object.class, ResourcePoolsBuilder.heap(settings.getMaxEntriesLocalHeap()))
					.withExpiry(ExpiryPolicyBuilder.expiry()
							.create(secondsToDuration(settings.getTimeToLiveSeconds()))
							.access(secondsToDuration(settings.getTimeToIdleSeconds()))
							.update(secondsToDuration(settings.getTimeToLiveSeconds()))
							.build()
					);

			return Eh107Configuration.fromEhcacheCacheConfiguration(config);
		};
	}

	@Bean
	public CacheSettingsProvider ehCacheSettingsProvider()
	{
		return new CacheSettingsProvider(EhCacheConfigParser.parse());
	}

	private Duration secondsToDuration(int seconds)
	{
		return Duration.ofSeconds(seconds == 0 ? Long.MAX_VALUE : seconds);
	}
}
