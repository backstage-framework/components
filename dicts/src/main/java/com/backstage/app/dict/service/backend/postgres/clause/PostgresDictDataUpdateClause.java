/*
 *    Copyright 2019-2025 the original author or authors.
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
import com.backstage.app.dict.constant.ServiceFieldConstants;
import com.backstage.app.dict.domain.Dict;
import com.backstage.app.dict.domain.DictField;
import com.backstage.app.dict.service.DictService;
import com.backstage.app.dict.service.backend.postgres.PostgresReservedKeyword;
import com.backstage.app.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;

import java.lang.reflect.Array;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PostgresDictDataUpdateClause
{
	private final DictService dictService;

	private final PostgresReservedKeyword reservedKeyword;

	public void addUpdateClause(String column, Object oldValue, Object newValue,
	                            LinkedHashSet<String> updateClauses, MapSqlParameterSource sqlParameterSource)
	{
		if (Objects.equals(oldValue, newValue))
		{
			return;
		}

		var paramName = sqlParamName(column);

		updateClauses.add("%s = :%s".formatted(reservedKeyword.postgresWord(column).getQuotedIfKeyword(), paramName));
		sqlParameterSource.addValue(paramName, newValue);
	}

	public void addUpdateJsonClause(String column, Object oldValue, Object newValue,
	                                LinkedHashSet<String> updateClauses, MapSqlParameterSource sqlParameterSource)
	{
		addUpdateClause(column, jsonValue(oldValue), jsonValue(newValue), updateClauses, sqlParameterSource);
	}

	public void addUpdateJsonArrayClause(String column, Object oldValue, Object newValue,
	                                LinkedHashSet<String> updateClauses, MapSqlParameterSource sqlParameterSource)
	{
		PGobject[] oldArrayValue = ((List<?>) oldValue).stream()
				.map(this::jsonValue)
				.toArray(PGobject[]::new);

		PGobject[] newArrayValue = ((List<?>) newValue).stream()
				.map(this::jsonValue)
				.toArray(PGobject[]::new);

		addUpdateClause(column, oldArrayValue, newArrayValue, updateClauses, sqlParameterSource);
	}

	public void addDictDataUpdateClause(Dict dict, Map<String, Object> oldData, Map<String, Object> newData,
	                                    LinkedHashSet<String> updateClauses, MapSqlParameterSource sqlParameterSource)
	{
		var fieldMap = dictService.getDataFieldsByDict(dict)
				.stream()
				.collect(Collectors.toMap(DictField::getId, Function.identity()));

		newData.entrySet()
				.stream()
				.filter(it -> !StringUtils.equals(it.getKey(), ServiceFieldConstants.ID))
				.forEach(it -> {
					var field = fieldMap.get(it.getKey());
					var oldValue = oldData.getOrDefault(it.getKey(), null);

					if (field.isMultivalued())
					{
						completeMultiValue(field, it.getKey(), oldValue, it.getValue(), updateClauses, sqlParameterSource);

						return;
					}

					completeSingleValue(field, it.getKey(), oldValue, it.getValue(), updateClauses, sqlParameterSource);
				});
	}

	@SuppressWarnings("unchecked")
	private void completeMultiValue(DictField field, String column, Object oldValue, Object newValue,
	                                LinkedHashSet<String> updateClauses, MapSqlParameterSource sqlParameterSource)
	{
		if (DictFieldType.JSON.equals(field.getType()))
		{
			completeSingleValue(field, column, oldValue, newValue, updateClauses, sqlParameterSource);

			return;
		}

		if (DictFieldType.GEO_JSON.equals(field.getType()))
		{
			addUpdateJsonArrayClause(column, oldValue, newValue, updateClauses, sqlParameterSource);

			return;
		}

		// FIXME: без каста работает некорректно - не отрабатывает автокаст в пг. Разобраться, исправить и убрать костыль
		if (field.getType() == DictFieldType.TIMESTAMP)
		{
			oldValue = ((List<LocalDateTime>) oldValue).stream()
					.map(Timestamp::valueOf)
					.toList();

			newValue = ((List<LocalDateTime>) newValue).stream()
					.map(Timestamp::valueOf)
					.toList();
		}

		if (oldValue instanceof List<?> listValue)
		{
			oldValue = listValue.isEmpty()
					? null
					: listValue.toArray(size -> (Object[]) Array.newInstance(listValue.get(0).getClass(), size));
		}

		if (newValue instanceof List<?> listValue)
		{
			newValue = listValue.isEmpty()
					? null
					: listValue.toArray(size -> (Object[]) Array.newInstance(listValue.get(0).getClass(), size));
		}

		completeSingleValue(field, column, oldValue, newValue, updateClauses, sqlParameterSource);
	}

	private void completeSingleValue(DictField field, String column, Object oldValue, Object newValue,
	                                 LinkedHashSet<String> updateClauses, MapSqlParameterSource sqlParameterSource)
	{
		if (DictFieldType.JSON.equals(field.getType()) || DictFieldType.GEO_JSON.equals(field.getType()))
		{
			addUpdateJsonClause(column, oldValue, newValue, updateClauses, sqlParameterSource);

			return;
		}

		if (DictFieldType.DECIMAL.equals(field.getType()))
		{
			sqlParameterSource.registerSqlType(sqlParamName(column), Types.DECIMAL);
		}

		addUpdateClause(column, oldValue, newValue, updateClauses, sqlParameterSource);
	}

	protected String sqlParamName(String column)
	{
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
