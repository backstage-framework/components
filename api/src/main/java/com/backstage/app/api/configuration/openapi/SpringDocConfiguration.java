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

package com.backstage.app.api.configuration.openapi;

import com.backstage.app.api.configuration.conditional.ConditionalOnOpenApi;
import com.backstage.app.api.configuration.openapi.model.Pageable;
import com.backstage.app.api.configuration.openapi.model.Sort;
import com.backstage.app.api.configuration.properties.ApiProperties;
import com.backstage.app.configuration.properties.AppProperties;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.converters.PageableOpenAPIConverter;
import org.springdoc.core.converters.SortOpenAPIConverter;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration
@ConditionalOnOpenApi
@RequiredArgsConstructor
public class SpringDocConfiguration extends org.springdoc.core.configuration.SpringDocConfiguration implements BeanPostProcessor
{
	private final AppProperties appProperties;
	private final ApiProperties apiProperties;

	@Bean
	ObjectMapperProvider springDocObjectMapperProvider(SpringDocConfigProperties springDocConfigProperties)
	{
		return new ObjectMapperProvider(springDocConfigProperties);
	}

	@Bean
	public PageableOpenAPIConverter customPageableOpenAPIConverter(ObjectMapperProvider objectMapperProvider)
	{
		SpringDocUtils.getConfig()
				.replaceParameterObjectWithClass(org.springframework.data.domain.Pageable.class, Pageable.class)
				.replaceParameterObjectWithClass(org.springframework.data.domain.PageRequest.class, Pageable.class);

		return new PageableOpenAPIConverter(objectMapperProvider);
	}

	@Bean
	public SortOpenAPIConverter customSortOpenAPIConverter(ObjectMapperProvider objectMapperProvider)
	{
		SpringDocUtils.getConfig()
				.replaceParameterObjectWithClass(org.springframework.data.domain.Sort.class, Sort.class);

		return new SortOpenAPIConverter(objectMapperProvider);
	}

	@Bean
	public OpenAPI apiDocumentation()
	{
		SpringDocUtils.getConfig()
				.removeRequestWrapperToIgnore(java.util.Map.class);

		return new OpenAPI()
				.info(new Info()
						.title(appProperties.getModule())
						.version(appProperties.getVersion()));
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException
	{
		if (bean instanceof SpringDocConfigProperties springDocConfigProperties)
		{
			Optional.ofNullable(apiProperties.getOpenapi())
					.ifPresent(swaggerProperties -> {
						Optional.ofNullable(swaggerProperties.getPackagesToScan())
								.ifPresent(springDocConfigProperties::setPackagesToScan);

						Optional.ofNullable(swaggerProperties.getPathsToMatch())
								.ifPresent(springDocConfigProperties::setPathsToMatch);
					});
		}
		else if (bean instanceof SwaggerUiConfigProperties swaggerUiConfigProperties)
		{
			swaggerUiConfigProperties.setDocExpansion("none");
			swaggerUiConfigProperties.setTagsSorter("alpha");

			Optional.ofNullable(apiProperties.getOpenapi())
					.ifPresent(swaggerProperties -> swaggerUiConfigProperties.getCsrf().setEnabled(swaggerProperties.isCsrf()));
		}

		return bean;
	}
}
