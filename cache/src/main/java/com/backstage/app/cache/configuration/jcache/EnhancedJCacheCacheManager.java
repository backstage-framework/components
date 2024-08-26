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

package com.backstage.app.cache.configuration.jcache;

import com.backstage.app.cache.configuration.CacheDecorator;
import lombok.Setter;
import org.springframework.cache.Cache;
import org.springframework.cache.jcache.JCacheCacheManager;

public class EnhancedJCacheCacheManager extends JCacheCacheManager
{
	@Setter
	private CacheDecorator cacheDecorator;

	@Override
	protected Cache decorateCache(Cache cache)
	{
		var result = isTransactionAware() ? new EnhancedTransactionAwareCacheDecorator(cache) : cache;

		return (cacheDecorator != null) ? cacheDecorator.decorate(result) : result;
	}
}
