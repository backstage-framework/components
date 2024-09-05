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

package com.backstage.app.dict.service.query;

import com.backstage.app.dict.constant.ServiceFieldConstants;
import com.backstage.app.dict.domain.Dict;
import com.backstage.app.dict.exception.dict.field.FieldNotFoundException;
import com.backstage.app.dict.model.postgres.backend.PostgresWord;
import com.backstage.app.dict.model.postgres.query.PostgresQuery;
import com.backstage.app.dict.model.postgres.query.PostgresQueryField;
import com.backstage.app.dict.model.postgres.query.PostgresTranslationContext;
import com.backstage.app.dict.service.backend.postgres.PostgresReservedKeyword;
import com.backstage.app.dict.service.query.ast.*;
import com.backstage.app.exception.AppException;
import com.backstage.app.model.other.exception.ApiStatusCodeImpl;
import com.backstage.app.utils.JsonUtils;
import com.google.common.collect.Sets;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;

@RequiredArgsConstructor
public class PostgresTranslator implements Translator<PostgresQuery>, Visitor<PostgresQuery, PostgresTranslationContext>
{
	private final PostgresReservedKeyword reservedKeyword;

	@Override
	public PostgresQuery process(Dict dict, QueryExpression queryExpression)
	{
		return queryExpression.process(this, new PostgresTranslationContext(dict));
	}

	@Override
	public PostgresQuery visit(Constant constant, PostgresTranslationContext context)
	{
		return PostgresQuery.builder()
				.value(constant.getValue())
				.build();
	}

	@Override
	public PostgresQuery visit(Field field, PostgresTranslationContext context)
	{
		context.getDict()
				.getFields()
				.stream()
				.filter(it -> it.getId().equalsIgnoreCase(field.name) || Set.of(ServiceFieldConstants.ID, ServiceFieldConstants._ID).contains(field.name))
				.findFirst()
				.orElseThrow(() -> new FieldNotFoundException(context.getDict().getId(), field.name));

		return PostgresQuery.builder()
				.field(new PostgresQueryField(field.dictId == null ? context.getDict().getId() : field.dictId, field.name, reservedKeyword))
				.build();
	}

	@Override
	public PostgresQuery visit(LikeQueryExpression expression, PostgresTranslationContext context)
	{
		var field = expression.field.process(this, context).getField();
		var paramName = paramName(field, context);

		var template = expression.template.process(this, context).getValue();
		var paramValue = ".*%s.*".formatted(template);

		var parameterSource = new MapSqlParameterSource(paramName, paramValue);
		var sqlExpression = "%s ~ :%s".formatted(field.getJoint(), paramName);

		return PostgresQuery.builder()
				.sqlExpression(sqlExpression)
				.participantDictIds(participantDictIds(field))
				.parameterSource(parameterSource)
				.build();
	}

	@Override
	public PostgresQuery visit(IlikeQueryExpression expression, PostgresTranslationContext context)
	{
		var field = expression.field.process(this, context).getField();
		var paramName = paramName(field, context);

		var template = expression.template.process(this, context).getValue();
		var paramValue = ".*%s.*".formatted(template);

		var parameterSource = new MapSqlParameterSource(paramName, paramValue);
		var sqlExpression = "%s ~* :%s".formatted(field.getJoint(), paramName);

		return PostgresQuery.builder()
				.sqlExpression(sqlExpression)
				.participantDictIds(participantDictIds(field))
				.parameterSource(parameterSource)
				.build();
	}

	@Override
	public PostgresQuery visit(LogicQueryExpression expression, PostgresTranslationContext context)
	{
		var left = expression.left.process(this, context);
		var right = expression.right.process(this, context);

		var leftExpression = left.getSqlExpression();
		var rightExpression = right.getSqlExpression();

		var parameterSource = new MapSqlParameterSource()
				.addValues(left.getParameterSource().getValues())
				.addValues(right.getParameterSource().getValues());

		var participantDictIds = Sets.union(left.getParticipantDictIds(), right.getParticipantDictIds());

		var query = switch (expression.type)
		{
			case AND -> "(%s and %s)".formatted(leftExpression, rightExpression);
			case OR -> "(%s or %s)".formatted(leftExpression, rightExpression);
			case NOT -> "not (%s)".formatted(leftExpression);

			default -> throw new AppException(ApiStatusCodeImpl.ILLEGAL_INPUT, "Не поддерживаемый оператор '%s'".formatted(expression.type));
		};

		return PostgresQuery.builder()
				.sqlExpression(query)
				.participantDictIds(participantDictIds)
				.parameterSource(parameterSource)
				.build();
	}

	@Override
	public PostgresQuery visit(Predicate predicate, PostgresTranslationContext context)
	{
		var left = predicate.left.process(this, context).getField();
		var right = predicate.right.process(this, context);

		var sqlExpression = left.getJoint();
		var paramName = paramName(left, context);

		switch (predicate.type)
		{
			case EQ -> sqlExpression += buildEqualsExpression(right, paramName);
			case NEQ -> sqlExpression += buildNotEqualsExpression(right, paramName);
			case LS -> sqlExpression += " < :%s".formatted(paramName);
			case GT -> sqlExpression += " > :%s".formatted(paramName);
			case LEQ -> sqlExpression += " <= :%s".formatted(paramName);
			case GEQ -> sqlExpression += " >= :%s".formatted(paramName);

			default -> throw new AppException(ApiStatusCodeImpl.ILLEGAL_INPUT, "Не поддерживаемый оператор '%s'".formatted(predicate.type));
		}

		return PostgresQuery.builder()
				.sqlExpression(sqlExpression)
				.participantDictIds(participantDictIds(left))
				.parameterSource(new MapSqlParameterSource(paramName, right.getValue()))
				.build();
	}

	@Override
	public PostgresQuery visit(InQueryExpression expression, PostgresTranslationContext context)
	{
		var left = expression.field.process(this, context).getField();
		var variants = expression.variants.stream()
				.map(it -> it.process(this, context))
				.map(PostgresQuery::getValue)
				.toList();

		var paramName = paramName(left, context);
		var sqlExpression = "%s in (:%s)".formatted(left.getJoint(), paramName);
		var parameterSource = new MapSqlParameterSource(paramName, variants);

		return PostgresQuery.builder()
				.sqlExpression(sqlExpression)
				.participantDictIds(participantDictIds(left))
				.parameterSource(parameterSource)
				.build();
	}

	@Override
	public PostgresQuery visit(AllOrAnyQueryExpression expression, PostgresTranslationContext context)
	{
		var left = expression.field.process(this, context).getField();
		var constants = expression.constants.stream()
				.map(it -> it.process(this, context))
				.map(PostgresQuery::getValue)
				.map(this::getCastedParamValue)
				.toList();

		var sqlExpression = left.getJoint();
		var paramName = paramName(left, context);
		var parameterSource = new MapSqlParameterSource()
				.addValue(paramName, constants);

		switch (expression.type)
		{
			case ALL -> sqlExpression += " %s ARRAY[:%s]%s".formatted("@>", paramName, getArrayCastingType(constants.get(0)));
			case ANY -> sqlExpression += " %s ARRAY[:%s]%s".formatted("&&", paramName, getArrayCastingType(constants.get(0)));

			default -> throw new AppException(ApiStatusCodeImpl.ILLEGAL_INPUT, "Не поддерживаемый оператор '%s'".formatted(expression.type));
		}

		return PostgresQuery.builder()
				.sqlExpression(sqlExpression)
				.participantDictIds(participantDictIds(left))
				.parameterSource(parameterSource)
				.build();
	}

	@Override
	public PostgresQuery visit(Empty empty, PostgresTranslationContext context)
	{
		return PostgresQuery.builder().build();
	}

	private Set<PostgresWord> participantDictIds(PostgresQueryField field)
	{
		return field.getDictId() != null ? Set.of(field.getDictId()) : Collections.emptySet();
	}

	private String paramName(PostgresQueryField field, PostgresTranslationContext context)
	{
		return field.getFieldId().getOriginalWord() + context.newQueryParamNumber();
	}

	private String buildEqualsExpression(PostgresQuery right, String paramName)
	{
		return right.getValue() == null
				? " is null"
				: " = :%s".formatted(paramName);
	}

	private String buildNotEqualsExpression(PostgresQuery right, String paramName)
	{
		return right.getValue() == null
				? " is not null"
				: " != :%s".formatted(paramName);
	}

	private String getArrayCastingType(Object value)
	{
		if (value instanceof Date)
		{
			return "::date[]";

		}

		if (value instanceof Timestamp)
		{
			return "::timestamp[]";
		}

		if (value instanceof String)
		{
			return "::text[]";
		}

		return StringUtils.EMPTY;
	}

	private Object getCastedParamValue(Object value)
	{
		if (value instanceof LocalDate localDate)
		{
			return Date.valueOf(localDate);
		}

		if (value instanceof LocalDateTime localDateTime)
		{
			return Timestamp.valueOf(localDateTime);
		}

		if (value instanceof Number)
		{
			return value;
		}

		if (value instanceof String)
		{
			return value;
		}

		return JsonUtils.toJson(value);
	}
}
