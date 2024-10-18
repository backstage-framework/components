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

package com.backstage.app.database.configuration.ddl;

import com.backstage.app.utils.MapUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DDLProviderInitializer implements InitializingBean, Ordered
{
	private final Map<String, DDLProvider> ddlProviders;

	@Getter
	private final int order = 0;

	@Override
	public void afterPropertiesSet()
	{
		ddlProviders.values()
				.stream()
				.sorted(AnnotationAwareOrderComparator.INSTANCE)
				.peek(provider -> log.info("Applying DDL with '{}'...", MapUtils.getKeyByValue(ddlProviders, provider)))
				.forEach(DDLProvider::update);
	}
}
