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

import com.backstage.app.dict.api.dto.DictDto;
import com.backstage.app.dict.api.dto.DictFieldDto;
import com.backstage.app.dict.api.dto.DictFieldNameDto;
import com.google.common.base.CaseFormat;
import lombok.experimental.UtilityClass;
import org.geojson.GeoJsonObject;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.ParameterizedTypeName;
import org.springframework.javapoet.TypeName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@UtilityClass
public class DictModelNameUtils
{
	@UtilityClass
	class TypeNames
	{
		final ClassName STRING = ClassName.get("java.lang", "String");
		final ClassName INTEGER = ClassName.get(Integer.class);
		final ClassName LONG = ClassName.get(Long.class);
		final ClassName BOOLEAN = ClassName.get(Boolean.class);
		final ClassName BIG_DECIMAL = ClassName.get(BigDecimal.class);
		final ClassName LOCAL_DATE = ClassName.get(LocalDate.class);
		final ClassName LOCAL_DATE_TIME = ClassName.get(LocalDateTime.class);
		final ClassName LIST = ClassName.get("java.util", "List");
		final ClassName MAP = ClassName.get("java.util", "Map");
		final TypeName JSON = ParameterizedTypeName.get(MAP, STRING, TypeName.OBJECT);
		final ClassName GEO_JSON = ClassName.get(GeoJsonObject.class);
	}

	public String className(DictDto dict)
	{
		return className(dict.getId()) + "DictItem";
	}

	public String className(DictFieldNameDto dictRef)
	{
		return className(dictRef.getDictId()) + "DictItem";
	}

	public String className(String id)
	{
		if (id.contains("_"))
		{
			return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, id);
		}
		else
		{
			return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, id);
		}
	}

	public String enumConstantName(String id)
	{
		return id;
	}

	public String constantName(String id)
	{
		return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, fieldName(id));
	}

	public String fieldName(String id)
	{
		if (id.contains("_"))
		{
			return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, id);
		}
		else
		{
			return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, id);
		}
	}

	public TypeName fieldTypeName(DictFieldDto dictField)
	{
		var typeName = switch (dictField.getType())
		{
			case DICT, STRING, ATTACHMENT -> TypeNames.STRING;
			// FIXME: убедиться, что возвращается long
			case INTEGER -> TypeNames.LONG;
			case BOOLEAN -> TypeNames.BOOLEAN;
			case DATE -> TypeNames.LOCAL_DATE;
			case TIMESTAMP -> TypeNames.LOCAL_DATE_TIME;
			case DECIMAL -> TypeNames.BIG_DECIMAL;
			case ENUM -> ClassName.bestGuess(className(dictField.getEnumId()));
			case JSON -> TypeNames.JSON;
			case GEO_JSON -> TypeNames.GEO_JSON;
			default -> TypeName.OBJECT;
		};

		if (dictField.isMultivalued())
		{
			return ParameterizedTypeName.get(TypeNames.LIST, typeName);
		}

		return typeName;
	}

	public String getterName(String propertyName)
	{
		return methodName("get", propertyName);
	}

	public String methodName(String method, String propertyName)
	{
		return method + CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, propertyName);
	}
}
