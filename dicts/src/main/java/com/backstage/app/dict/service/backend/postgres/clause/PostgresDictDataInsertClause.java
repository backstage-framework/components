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

package com.backstage.app.dict.service.backend.postgres.clause;

import com.backstage.app.dict.api.domain.DictFieldType;
import com.backstage.app.dict.domain.Dict;
import com.backstage.app.dict.domain.DictField;
import com.backstage.app.dict.model.postgres.backend.PostgresDictItem;
import com.backstage.app.dict.service.DictService;
import com.backstage.app.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;

import java.lang.reflect.Array;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PostgresDictDataInsertClause
{
	public void addInsertClause(String column, Object value, LinkedHashSet<String> columns, MapSqlParameterSource sqlParameterSource)
	{
		columns.add(column);

		sqlParameterSource.addValue(sqlParamName(column), value);
	}

	public void addInsertJsonClause(String columnPlaceholder, Object value, LinkedHashSet<String> columns, MapSqlParameterSource sqlParameterSource)
	{
		addInsertClause(columnPlaceholder, jsonValue(value), columns, sqlParameterSource);
	}

	public void addDictDataInsertClause(Dict dict, PostgresDictItem dictItem, LinkedHashSet<String> columns, MapSqlParameterSource sqlParameterSource)
	{
		var fieldMap = DictService.getDataFieldsByDict(dict)
				.stream()
				.collect(Collectors.toMap(DictField::getId, Function.identity()));

		dictItem.getDictData()
				.forEach((column, value) -> {
					var mapKeyColumn = column.replace("\"", "");

					var field = fieldMap.get(mapKeyColumn);

					if (field.isMultivalued())
					{
						completeMultiValue(field, column, value, columns, sqlParameterSource);

						return;
					}

					completeSingleValue(field, column, value, columns, sqlParameterSource);
				});
	}

	@SuppressWarnings("unchecked")
	private void completeMultiValue(DictField field, String column, Object value, LinkedHashSet<String> columns, MapSqlParameterSource sqlParameterSource)
	{
		// FIXME: работает неправильно, должен быть JSON[].
		if (DictFieldType.JSON.equals(field.getType()))
		{
			completeSingleValue(field, column, value, columns, sqlParameterSource);

			return;
		}

		// FIXME: без каста работает некорректно - не отрабатывает автокаст в пг. Разобраться, исправить и убрать костыль
		if (field.getType() == DictFieldType.TIMESTAMP)
		{
			value = ((List<LocalDateTime>) value).stream()
					.map(Timestamp::valueOf)
					.toList();
		}

		if (value instanceof List<?> listValue)
		{
			value = listValue.isEmpty()
					? null
					: listValue.toArray(size -> (Object[]) Array.newInstance(listValue.get(0).getClass(), size));
		}

		completeSingleValue(field, column, value, columns, sqlParameterSource);
	}

	private void completeSingleValue(DictField field, String column, Object value, LinkedHashSet<String> columns, MapSqlParameterSource sqlParameterSource)
	{
		if (DictFieldType.JSON.equals(field.getType()))
		{
			addInsertJsonClause(column, value, columns, sqlParameterSource);

			return;
		}

		if (DictFieldType.DECIMAL.equals(field.getType()))
		{
			sqlParameterSource.registerSqlType(sqlParamName(column), Types.DECIMAL);
		}

		addInsertClause(column, value, columns, sqlParameterSource);
	}

	protected String sqlParamName(String column)
	{
		column = column.replaceAll("\"", "");

		return column + "Val";
	}

	@SneakyThrows
	private PGobject jsonValue(Object value)
	{
		PGobject jsonObject = new PGobject();
		jsonObject.setType("jsonb");
		jsonObject.setValue(JsonUtils.toJson(value));

		return jsonObject;
	}
}
