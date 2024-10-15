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

package com.backstage.app.database.configuration.jpa.eclipselink.schema;

import com.backstage.app.configuration.properties.AppProperties;
import com.backstage.app.utils.SpringContextUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.persistence.internal.helper.DatabaseTable;
import org.eclipse.persistence.mappings.DirectCollectionMapping;
import org.eclipse.persistence.mappings.ManyToManyMapping;
import org.eclipse.persistence.sequencing.Sequence;
import org.eclipse.persistence.sessions.SessionEvent;
import org.eclipse.persistence.sessions.SessionEventAdapter;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

@RequiredArgsConstructor
public class SpringAwareTableSchemaResolvingExtension extends SessionEventAdapter
{
	private static final String PLACEHOLDER_REGEX = "^\\$\\{([\\w.]+)}$";
	private static final String SPEL_TEMPLATE_REGEX = "^#\\{(.+)}$";

	private final ConfigurableBeanFactory beanFactory;

	private final AppProperties appProperties;

	private StandardEvaluationContext evaluationContext;

	@Override
	public void preLogin(final SessionEvent event)
	{
		event.getSession()
				.getDescriptors()
				.forEach((clazz, descriptor) -> {

					if (descriptor.getTables().isEmpty())
					{
						return;
					}

					descriptor.getMappings()
							.stream()
							.filter(it -> it instanceof ManyToManyMapping)
							.map(ManyToManyMapping.class::cast)
							.map(ManyToManyMapping::getRelationTable)
							.forEach(this::resolveTableScheme);

					descriptor.getMappings()
							.stream()
							.filter(it -> it instanceof DirectCollectionMapping)
							.map(DirectCollectionMapping.class::cast)
							.map(DirectCollectionMapping::getReferenceTable)
							.forEach(this::resolveTableScheme);

					descriptor.getTables()
							.forEach(this::resolveTableScheme);
				});
	}

	@Override
	public void postLogin(SessionEvent event)
	{
		event.getSession()
				.getDescriptors()
				.forEach((clazz, descriptor) -> {
					var sequence = descriptor.getSequence();

					if (sequence == null)
					{
						return;
					}

					resolveSequenceScheme(sequence);
				});
	}

	private void resolveSequenceScheme(Sequence sequence)
	{
		var schemaValue = sequence.getQualifier();
		var schema = getSchema(schemaValue);

		sequence.setQualifier(schema);
	}

	private void resolveTableScheme(DatabaseTable table)
	{
		var schemaValue = table.getTableQualifier();
		var schema = getSchema(schemaValue);

		table.setTableQualifier(schema);
	}

	private String getSchema(String schemaValue)
	{
		if (schemaValue.matches(PLACEHOLDER_REGEX))
		{
			var resolvedSchema = beanFactory.resolveEmbeddedValue(schemaValue);

			return schemaValue.equals(resolvedSchema)
					? StringUtils.EMPTY
					: resolvedSchema;
		}

		if (schemaValue.matches(SPEL_TEMPLATE_REGEX))
		{
			var resolvedSchema = getValueFromExpression(schemaValue);

			return resolvedSchema == null
					? StringUtils.EMPTY
					: resolvedSchema;
		}

		return schemaValue;
	}

	private String getValueFromExpression(String expression)
	{
		if (evaluationContext == null)
		{
			evaluationContext = new StandardEvaluationContext();
			evaluationContext.setBeanResolver(new BeanFactoryResolver(
					SpringContextUtils.exposeConfigurationProperties(appProperties.getBasePackages(), beanFactory)));
		}

		return new SpelExpressionParser().parseExpression(expression, new TemplateParserContext())
				.getValue(evaluationContext, String.class);
	}
}
