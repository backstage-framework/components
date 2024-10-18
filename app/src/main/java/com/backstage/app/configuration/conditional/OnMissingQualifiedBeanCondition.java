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

package com.backstage.app.configuration.conditional;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.util.ObjectUtils;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Дублирует часть кода из {@link org.springframework.boot.autoconfigure.condition.OnBeanCondition}, так как нет возможности расширить базовый функционал.
 */
public class OnMissingQualifiedBeanCondition extends SpringBootCondition
{
	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata)
	{
		if (hasMatchingBeans(context, metadata))
		{
			return ConditionOutcome.noMatch("there are matching beans");
		}

		return ConditionOutcome.match();
	}

	protected final boolean hasMatchingBeans(ConditionContext context, AnnotatedTypeMetadata metadata)
	{
		ClassLoader classLoader = context.getClassLoader();
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		boolean considerHierarchy = true;

		var beanTypes = deducedBeanType(context, metadata);
		var matchingBeanNamesByType = new LinkedHashSet<String>();

		for (String type : beanTypes)
		{
			matchingBeanNamesByType.addAll(getBeanNamesForType(classLoader, considerHierarchy, beanFactory, type));
		}

		String qualifierType;

		MergedAnnotation<?> beanQualifier = metadata.getAnnotations().get(Qualifier.class);

		// TODO: проверка на отсутствие Qualifier на методе и в аннотации, должна быть ошибка конфигурации.
		if (beanQualifier != MergedAnnotation.missing())
		{
			while (!beanQualifier.isDirectlyPresent())
			{
				beanQualifier = beanQualifier.getRoot();
			}

			qualifierType = beanQualifier.getType().getName();
		}
		else
		{
			qualifierType = metadata.getAnnotations().get(ConditionalOnMissingQualifiedBean.class).getString("value");
		}

		var matchingBeanNamesByQualifier = getBeanNamesForAnnotation(classLoader, beanFactory, qualifierType, true);

		return !matchingBeanNamesByQualifier.isEmpty() && matchingBeanNamesByType.removeAll(matchingBeanNamesByQualifier);
	}

	private Set<String> getBeanNamesForType(ClassLoader classLoader, boolean considerHierarchy, ListableBeanFactory beanFactory, String type) throws LinkageError
	{
		try
		{
			return getBeanNamesForType(beanFactory, considerHierarchy, resolve(type, classLoader));
		}
		catch (ClassNotFoundException | NoClassDefFoundError ex)
		{
			return Collections.emptySet();
		}
	}

	private Set<String> getBeanNamesForType(ListableBeanFactory beanFactory, boolean considerHierarchy, Class<?> type)
	{
		Set<String> result = collectBeanNamesForType(beanFactory, considerHierarchy, type, null);

		return (result != null) ? result : Collections.emptySet();
	}

	private Set<String> collectBeanNamesForType(ListableBeanFactory beanFactory, boolean considerHierarchy, Class<?> type, Set<String> result)
	{
		result = addAll(result, beanFactory.getBeanNamesForType(type, true, false));

		if (considerHierarchy && beanFactory instanceof HierarchicalBeanFactory hierarchicalBeanFactory)
		{
			BeanFactory parent = hierarchicalBeanFactory.getParentBeanFactory();
			if (parent instanceof ListableBeanFactory listableBeanFactory)
			{
				result = collectBeanNamesForType(listableBeanFactory, considerHierarchy, type, result);
			}
		}

		return result;
	}

	private Set<String> getBeanNamesForAnnotation(ClassLoader classLoader, ConfigurableListableBeanFactory beanFactory, String type, boolean considerHierarchy) throws LinkageError
	{
		Set<String> result = null;

		try
		{
			result = collectBeanNamesForAnnotation(beanFactory, resolveAnnotationType(classLoader, type), considerHierarchy, result);
		}
		catch (ClassNotFoundException ex)
		{
			// Continue
		}

		return (result != null) ? result : Collections.emptySet();
	}

	@SuppressWarnings("unchecked")
	private Class<? extends Annotation> resolveAnnotationType(ClassLoader classLoader, String type) throws ClassNotFoundException
	{
		return (Class<? extends Annotation>) resolve(type, classLoader);
	}

	private Set<String> collectBeanNamesForAnnotation(ListableBeanFactory beanFactory, Class<? extends Annotation> annotationType, boolean considerHierarchy, Set<String> result)
	{
		result = addAll(result, getBeanNamesForAnnotation(beanFactory, annotationType));

		if (considerHierarchy)
		{
			BeanFactory parent = ((HierarchicalBeanFactory) beanFactory).getParentBeanFactory();
			if (parent instanceof ListableBeanFactory listableBeanFactory)
			{
				result = collectBeanNamesForAnnotation(listableBeanFactory, annotationType, considerHierarchy, result);
			}
		}

		return result;
	}

	private String[] getBeanNamesForAnnotation(ListableBeanFactory beanFactory, Class<? extends Annotation> annotationType)
	{
		Set<String> foundBeanNames = new LinkedHashSet<>();

		for (String beanName : beanFactory.getBeanDefinitionNames())
		{
			if (beanFactory instanceof ConfigurableListableBeanFactory configurableListableBeanFactory)
			{
				BeanDefinition beanDefinition = configurableListableBeanFactory.getBeanDefinition(beanName);

				if (beanDefinition != null && beanDefinition.isAbstract())
				{
					continue;
				}
			}
			if (beanFactory.findAnnotationOnBean(beanName, annotationType, false) != null)
			{
				foundBeanNames.add(beanName);
			}
		}

		if (beanFactory instanceof SingletonBeanRegistry singletonBeanRegistry)
		{
			for (String beanName : singletonBeanRegistry.getSingletonNames())
			{
				if (beanFactory.findAnnotationOnBean(beanName, annotationType) != null)
				{
					foundBeanNames.add(beanName);
				}
			}
		}

		return foundBeanNames.toArray(String[]::new);
	}

	protected static Class<?> resolve(String className, ClassLoader classLoader) throws ClassNotFoundException
	{
		if (classLoader != null)
		{
			return Class.forName(className, false, classLoader);
		}

		return Class.forName(className);
	}

	private static Set<String> addAll(Set<String> result, String[] additional)
	{
		if (ObjectUtils.isEmpty(additional))
		{
			return result;
		}

		result = (result != null) ? result : new LinkedHashSet<>();
		Collections.addAll(result, additional);

		return result;
	}

	private Set<String> deducedBeanType(ConditionContext context, AnnotatedTypeMetadata metadata)
	{
		if (metadata instanceof MethodMetadata && metadata.isAnnotated(Bean.class.getName()))
		{
			return deducedBeanTypeForBeanMethod(context, (MethodMetadata) metadata);
		}

		return Collections.emptySet();
	}

	private Set<String> deducedBeanTypeForBeanMethod(ConditionContext context, MethodMetadata metadata)
	{
		try
		{
			Class<?> returnType = getReturnType(context, metadata);

			return Collections.singleton(returnType.getName());
		}
		catch (Throwable ex)
		{
			throw new RuntimeException(ex);
		}
	}

	private Class<?> getReturnType(ConditionContext context, MethodMetadata metadata) throws ClassNotFoundException, LinkageError
	{
		ClassLoader classLoader = context.getClassLoader();

		return resolve(metadata.getReturnTypeName(), classLoader);
	}
}