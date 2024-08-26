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

import com.backstage.app.dict.domain.DictItem;
import com.backstage.app.dict.service.DictDataService;
import com.backstage.app.dict.service.codegen.base.AbstractDictItemService;
import com.backstage.app.utils.DateUtils;
import jakarta.annotation.Generated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.javapoet.*;
import org.springframework.stereotype.Service;

import javax.lang.model.element.Modifier;
import java.lang.reflect.Type;
import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
public class DictItemServiceGenerator
{
	public TypeSpec generate(JavaFile model)
	{
		var modelSpec = model.typeSpec;
		var modelClassName = ClassName.get(model.packageName, modelSpec.name);

		return TypeSpec.classBuilder(getServiceClassName(model))
				.addModifiers(Modifier.PUBLIC)
				.superclass(ParameterizedTypeName.get(ClassName.get(getBaseType()), modelClassName))
				.addAnnotation(generatedAnnotation())
				.addAnnotation(Service.class)
				.addMethod(MethodSpec.constructorBuilder()
						.addModifiers(Modifier.PUBLIC)
						.addParameter(getDictDataServiceType(), "dictDataService")
						.addStatement("super($L)", "dictDataService")
						.build())
				.addMethod(MethodSpec.methodBuilder("getDictId")
						.addAnnotation(Override.class)
						.addModifiers(Modifier.PROTECTED)
						.returns(String.class)
						.addStatement("return $T.$L", modelClassName, DictItemModelGenerator.DICT_ID_FIELD)
						.build())
				.addMethod(MethodSpec.methodBuilder("buildItem")
						.addAnnotation(Override.class)
						.addModifiers(Modifier.PROTECTED)
						.returns(modelClassName)
						.addParameter(DictItem.class, "dictItem")
						.addStatement("return new $T($L)", modelClassName, "dictItem")
						.build())
				.build();
	}

	protected String getServiceClassName(JavaFile model)
	{
		return model.typeSpec.name + "Service";
	}

	protected Class<?> getBaseType()
	{
		return AbstractDictItemService.class;
	}

	protected Type getDictDataServiceType()
	{
		return DictDataService.class;
	}

	protected AnnotationSpec generatedAnnotation()
	{
		return AnnotationSpec.builder(Generated.class)
				.addMember("value", "$S", this.getClass().getName())
				.addMember("date", "$S", DateUtils.toZonedDateTime(LocalDateTime.now()))
				.build();
	}
}
