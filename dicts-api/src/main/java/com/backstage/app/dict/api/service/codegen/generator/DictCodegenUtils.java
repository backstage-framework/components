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

package com.backstage.app.dict.api.service.codegen.generator;

import com.backstage.app.utils.DateUtils;
import jakarta.annotation.Generated;
import lombok.experimental.UtilityClass;
import org.springframework.javapoet.AnnotationSpec;

import java.time.LocalDateTime;

@UtilityClass
public class DictCodegenUtils
{
	public AnnotationSpec generatedAnnotation(Object generator)
	{
		return AnnotationSpec.builder(Generated.class)
				.addMember("value", "$S", generator.getClass().getName())
				.addMember("date", "$S", DateUtils.toZonedDateTime(LocalDateTime.now()))
				.build();
	}
}
