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

package com.backstage.app.dict.service.codegen.generator;

import com.backstage.app.dict.service.codegen.base.AbstractDictItemEndpoint;
import com.backstage.app.utils.DateUtils;
import jakarta.annotation.Generated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.javapoet.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.lang.model.element.Modifier;
import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
public class DictItemEndpointGenerator
{
	public TypeSpec generate(JavaFile service, JavaFile model)
	{
		var modelSpec = model.typeSpec;
		var modelClassName = ClassName.get(model.packageName, modelSpec.name);
		var serviceSpec = service.typeSpec;
		var serviceClassName = ClassName.get(model.packageName, serviceSpec.name);
		var className = modelSpec.name + "Endpoint";

		return TypeSpec.classBuilder(className)
				.addModifiers(Modifier.PUBLIC)
				.superclass(ParameterizedTypeName.get(ClassName.get(AbstractDictItemEndpoint.class), modelClassName, serviceClassName))
				.addAnnotation(generatedAnnotation())
				.addAnnotation(RestController.class)
				.addAnnotation(AnnotationSpec.builder(RequestMapping.class)
						.addMember("value", "$S + $L.$L", "/api/dicts/", modelClassName, DictItemModelGenerator.FIELD_DICT_ID)
						.build())
				.addMethod(MethodSpec.constructorBuilder()
						.addParameter(serviceClassName, "dictItemService")
						.addStatement("super(dictItemService)")
						.build())
				.build();
	}

	protected AnnotationSpec generatedAnnotation()
	{
		return AnnotationSpec.builder(Generated.class)
				.addMember("value", "$S", this.getClass().getName())
				.addMember("date", "$S", DateUtils.toZonedDateTime(LocalDateTime.now()))
				.build();
	}
}
