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

import com.backstage.app.dict.api.domain.DictFieldType;
import com.backstage.app.dict.api.dto.DictDto;
import com.backstage.app.dict.api.dto.DictEnumDto;
import com.backstage.app.dict.api.dto.DictFieldDto;
import com.backstage.app.dict.domain.DictItem;
import com.backstage.app.dict.service.codegen.base.AbstractDictItem;
import com.backstage.app.utils.DateUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Generated;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.javapoet.*;

import javax.lang.model.element.Modifier;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
public class DictItemModelGenerator
{
	private static final Set<String> DEFAULT_FIELDS = Set.of(
			"created",
			"updated",
			"deleted",
			"deletionReason",
			"history",
			"version"
	);

	static final String DICT_ID_FIELD = "DICT_ID";

	public TypeSpec generate(DictDto dict)
	{
		var className = DictModelNameUtils.className(dict);

		var typeSpec = TypeSpec.classBuilder(className)
				.addModifiers(Modifier.PUBLIC, Modifier.FINAL)
				.addSuperinterface(AbstractDictItem.class)
				.addAnnotation(ClassName.get("lombok", "Getter"))
				.addAnnotation(ClassName.get("lombok", "Setter"))
				.addAnnotation(generatedAnnotation())
				.addAnnotation(AnnotationSpec.builder(Schema.class)
						.addMember("description", "$S", dict.getName())
						.build())
				.addTypes(dict.getEnums().stream()
						.map(this::addEnum)
						.toList());

		var fieldSpecMapping = new LinkedHashMap<FieldSpec, DictFieldDto>();

		typeSpec.addField(addConstant(DICT_ID_FIELD, dict.getId()));

		dict.getFields().forEach(dictField -> {
			fieldSpecMapping.put(addField(dictField), dictField);

			if (!DEFAULT_FIELDS.contains(dictField.getId()))
			{
				typeSpec.addField(addConstant(dictField));
			}
		});

		typeSpec.addFields(fieldSpecMapping.keySet());
		typeSpec.addMethod(addConstructor(fieldSpecMapping));
		typeSpec.addMethods(addFetchMethods(fieldSpecMapping));
		typeSpec.addMethod(addMethod(fieldSpecMapping));

		return typeSpec.build();
	}

	protected AnnotationSpec generatedAnnotation()
	{
		return AnnotationSpec.builder(Generated.class)
				.addMember("value", "$S", this.getClass().getName())
				.addMember("date", "$S", DateUtils.toZonedDateTime(LocalDateTime.now()))
				.build();
	}

	private MethodSpec addConstructor(Map<FieldSpec, DictFieldDto> fieldSpecMapping)
	{
		var methodSpec = MethodSpec.constructorBuilder()
				.addModifiers(Modifier.PUBLIC)
				.addParameter(DictItem.class, "dictItem");

		fieldSpecMapping.forEach((fieldSpec, dictField) -> {
			if (DEFAULT_FIELDS.contains(dictField.getId()))
			{
				methodSpec.addStatement("this.$N = dictItem.$L()", fieldSpec, DictModelNameUtils.getterName(fieldSpec.name));
			}
			else if (dictField.getType() == DictFieldType.ENUM)
			{
				methodSpec.addStatement("this.$N = $T.fromValue(($T) dictItem.getData().get($N))", fieldSpec, ClassName.bestGuess(DictModelNameUtils.className(dictField.getEnumId())), String.class, DictModelNameUtils.constantName(fieldSpec.name));
			}
			else
			{
				methodSpec.addStatement("this.$N = ($T) dictItem.getData().get($N)", fieldSpec, fieldSpec.type, DictModelNameUtils.constantName(fieldSpec.name));
			}
		});

		return methodSpec.build();
	}

	private List<MethodSpec> addFetchMethods(Map<FieldSpec, DictFieldDto> fieldSpecMapping)
	{
		var result = new ArrayList<MethodSpec>();

		fieldSpecMapping.forEach((fieldSpec, dictField) -> {
			if (dictField.getType() == DictFieldType.DICT)
			{
				var methodBuilder = MethodSpec.methodBuilder(DictModelNameUtils.methodName("fetch", dictField.getId()))
						.addModifiers(Modifier.PUBLIC)
						.addParameter(ClassName.bestGuess(DictModelNameUtils.className(dictField.getDictRef()) + "Service"), "itemService");

				String methodName = "getById";
				TypeName returnTypeName = ClassName.bestGuess(DictModelNameUtils.className(dictField.getDictRef()));

				if (dictField.isMultivalued())
				{
					methodName = "getByIds";
					returnTypeName = ParameterizedTypeName.get(DictModelNameUtils.TypeNames.LIST, returnTypeName);
				}

				methodBuilder
						.returns(returnTypeName)
						.addStatement("return itemService.$L($L())", methodName, DictModelNameUtils.getterName(fieldSpec.name));

				result.add(methodBuilder.build());
			}
		});

		return result;
	}

	private MethodSpec addMethod(Map<FieldSpec, DictFieldDto> fieldSpecMapping)
	{
		var methodSpec = MethodSpec.methodBuilder("toMap")
				.addAnnotation(Override.class)
				.addModifiers(Modifier.PUBLIC)
				.returns(ParameterizedTypeName.get(Map.class, String.class, Object.class))
				.addStatement("var dataMap = new $T<$T, $T>()", HashMap.class, String.class, Object.class);

		fieldSpecMapping.forEach((fieldSpec, dictField) -> {
			if (!DEFAULT_FIELDS.contains(dictField.getId()))
			{
				if (dictField.getType() == DictFieldType.ENUM)
				{
					methodSpec.addStatement("dataMap.put($L, ($L() != null) ? $L().getValue() : null)", DictModelNameUtils.constantName(fieldSpec.name), DictModelNameUtils.getterName(fieldSpec.name), DictModelNameUtils.getterName(fieldSpec.name));
				}
				else
				{
					methodSpec.addStatement("dataMap.put($L, $L())", DictModelNameUtils.constantName(fieldSpec.name), DictModelNameUtils.getterName(fieldSpec.name));
				}
			}
		});

		methodSpec.addStatement("return dataMap");

		return methodSpec.build();
	}

	private TypeSpec addEnum(DictEnumDto dictEnum)
	{
		var className = ClassName.bestGuess(DictModelNameUtils.className(dictEnum.getId()));

		var valueParameterSpec = ParameterSpec.builder(String.class, "value")
				.build();

		var enumSpec = TypeSpec.enumBuilder(className)
				.addJavadoc(dictEnum.getName())
				.addAnnotation(ClassName.get("lombok", "Getter"))
				.addAnnotation(ClassName.get("lombok", "RequiredArgsConstructor"))
				.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
				.addField(String.class, valueParameterSpec.name, Modifier.PRIVATE, Modifier.FINAL)
				.addMethod(MethodSpec.methodBuilder("fromValue")
						.returns(className)
						.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
						.addParameter(valueParameterSpec)
						.addStatement("return $T.stream($T.values()).filter(it -> it.getValue().equals($N)).findFirst().orElse(null)", Arrays.class, className, valueParameterSpec)
						.build());

		dictEnum.getValues().forEach(value -> {
			enumSpec.addEnumConstant(DictModelNameUtils.enumConstantName(value), TypeSpec.anonymousClassBuilder("$S", value)
					.build());
		});

		return enumSpec.build();
	}

	private FieldSpec addField(DictFieldDto dictField)
	{
		var fieldSpec = FieldSpec.builder(DictModelNameUtils.fieldTypeName(dictField), DictModelNameUtils.fieldName(dictField.getId()))
				.addModifiers(Modifier.PRIVATE)
				.addAnnotation(AnnotationSpec.builder(Schema.class)
						.addMember("description", "$S", dictField.getName())
						.build());

		if (dictField.isRequired())
		{
			fieldSpec.addAnnotation(ClassName.get(NotNull.class));
		}

		if (dictField.getMinSize() != null)
		{
			fieldSpec.addAnnotation(AnnotationSpec.builder(ClassName.get(Min.class))
							.addMember("value", "$L", dictField.getMinSize())
					.build() );
		}

		if (dictField.getMaxSize() != null)
		{
			fieldSpec.addAnnotation(AnnotationSpec.builder(ClassName.get(Max.class))
					.addMember("value", "$L", dictField.getMaxSize())
					.build() );
		}

		return fieldSpec.build();
	}

	private FieldSpec addConstant(DictFieldDto dictField)
	{
		return addConstant(DictModelNameUtils.constantName(dictField.getId()), dictField.getId());
	}

	private FieldSpec addConstant(String name, String value)
	{
		return FieldSpec.builder(String.class, name)
				.addModifiers(Modifier.STATIC, Modifier.FINAL)
				.initializer("$S", value)
				.build();
	}
}
