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

package com.backstage.app.configuration;

import com.backstage.app.configuration.properties.AppProperties;
import com.backstage.app.model.other.user.SpringPrincipal;
import com.backstage.app.service.user.PermissionService;
import com.backstage.app.utils.SecurityUtils;
import lombok.Getter;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.util.List;

@Configuration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@EnableConfigurationProperties({AppProperties.class})
public class AppConfiguration implements ApplicationContextAware
{
	@Getter
	private static ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
	{
		AppConfiguration.applicationContext = applicationContext;
	}

	@Bean
	@ConditionalOnMissingBean
	public PermissionService defaultPermissionService()
	{
		return new PermissionService() {
			@Override
			public List<String> getPermissions(String userId)
			{
				var currentUser = SecurityUtils.getCurrentUser();

				if (currentUser.getId().equals(userId) && currentUser instanceof SpringPrincipal springPrincipal)
				{
					return springPrincipal.getPermissions();
				}

				return List.of();
			}

			@Override
			public List<String> getPermissions()
			{
				return getPermissions(SecurityUtils.getCurrentUserId());
			}
		};
	}

	@Bean
	static BeanFactoryPostProcessor taskExecutorAliasBeanFactoryPostProcessor()
	{
		return (beanFactory) -> beanFactory.registerAlias("applicationTaskExecutor", "taskExecutor");
	}
}
