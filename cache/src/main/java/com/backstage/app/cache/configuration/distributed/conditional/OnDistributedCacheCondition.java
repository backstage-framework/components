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

package com.backstage.app.cache.configuration.distributed.conditional;

import com.backstage.app.cache.configuration.distributed.DistributedCacheSettings;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Conditional;

public class OnDistributedCacheCondition extends AnyNestedCondition
{
	OnDistributedCacheCondition()
	{
		super(ConfigurationPhase.REGISTER_BEAN);
	}

	@Conditional(OnDistributedCachePropertiesCondition.class)
	private static final class OnDistributedCacheProperties
	{
	}

	@ConditionalOnBean(DistributedCacheSettings.class)
	private static final class OnDistributedCacheSettings
	{
	}
}