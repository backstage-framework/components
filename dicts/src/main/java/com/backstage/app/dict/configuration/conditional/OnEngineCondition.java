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

package com.backstage.app.dict.configuration.conditional;

import com.backstage.app.dict.configuration.properties.DictsProperties;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Arrays;

@Order(Ordered.HIGHEST_PRECEDENCE + 40)
public class OnEngineCondition extends SpringBootCondition
{
	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata)
	{
		var properties = context.getEnvironment()
				.getProperty(DictsProperties.ENGINE_PROPERTY, String[].class);

		if (properties == null)
		{
			return ConditionOutcome.noMatch("Не найден параметр '%s'".formatted(DictsProperties.ENGINE_PROPERTY));
		}

		var targetValue = metadata.getAnnotations()
				.get(ConditionalOnEngine.class)
				.getValue("value", String.class)
				.orElseThrow();

		return Arrays.asList(properties).contains(targetValue)
				? ConditionOutcome.match()
				: ConditionOutcome.noMatch("Значение '%s' для параметра '%s' не найдено".formatted("postgres", DictsProperties.ENGINE_PROPERTY));
	}
}
