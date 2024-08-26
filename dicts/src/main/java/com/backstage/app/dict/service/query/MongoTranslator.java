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
import com.backstage.app.dict.model.mongo.query.MongoQuery;
import com.backstage.app.dict.model.mongo.query.MongoQueryField;
import com.backstage.app.dict.model.query.TranslationContext;
import com.backstage.app.dict.service.query.ast.*;
import com.backstage.app.exception.AppException;
import com.backstage.app.model.other.exception.ApiStatusCodeImpl;
import com.google.common.collect.Sets;
import lombok.RequiredArgsConstructor;
import org.bson.types.Decimal128;
import org.springframework.data.mongodb.core.query.Criteria;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Set;

@RequiredArgsConstructor
public class MongoTranslator implements Translator<MongoQuery>, Visitor<MongoQuery, TranslationContext>
{
	@Override
	public MongoQuery process(Dict dict, QueryExpression queryExpression)
	{
		return queryExpression.process(this, new TranslationContext(dict));
	}

	@Override
	public MongoQuery visit(Constant constant, TranslationContext context)
	{
		var value = constant.getValue();

		//TODO: убрать после реализации валидации/маппинга QueryExpression для адаптеров
		if (Constant.Type.DECIMAL.equals(constant.type))
		{
			value = new Decimal128((BigDecimal) constant.getValue());
		}

		return MongoQuery.builder()
				.value(value)
				.build();
	}

	@Override
	public MongoQuery visit(Field field, TranslationContext context)
	{
		context.getDict()
				.getFields()
				.stream()
				.filter(it -> it.getId().equalsIgnoreCase(field.name) || Set.of(ServiceFieldConstants.ID, ServiceFieldConstants._ID).contains(field.name))
				.findFirst()
				.orElseThrow(() -> new FieldNotFoundException(context.getDict().getId(), field.name));

		var fieldName = field.name.equals(ServiceFieldConstants.ID) ? ServiceFieldConstants._ID : field.name;

		return MongoQuery.builder()
				.field(new MongoQueryField(field.dictId == null ? context.getDict().getId() : field.dictId, fieldName))
				.build();
	}

	@Override
	public MongoQuery visit(LikeQueryExpression expression, TranslationContext context)
	{
		var field = expression.field.process(this, context).getField();
		var template = expression.template.process(this, context).getValue();

		var criteria = Criteria.where(field.getFieldId())
				.regex(template.toString());

		return MongoQuery.builder()
				.criteria(criteria)
				.participantDictIds(participantDictIds(field))
				.build();
	}

	@Override
	public MongoQuery visit(IlikeQueryExpression expression, TranslationContext context)
	{
		var field = expression.field.process(this, context).getField();
		var template = expression.template.process(this, context).getValue();

		var criteria = Criteria.where(field.getFieldId())
				.regex(template.toString(), "i");

		return MongoQuery.builder()
				.criteria(criteria)
				.participantDictIds(participantDictIds(field))
				.build();
	}

	@Override
	public MongoQuery visit(LogicQueryExpression expression, TranslationContext context)
	{
		var left = expression.left.process(this, context);
		var right = expression.right.process(this, context);

		var participantDictIds = Sets.union(left.getParticipantDictIds(), right.getParticipantDictIds());

		var criteria = switch (expression.type)
		{
			case OR -> new Criteria().orOperator(left.getCriteria(), right.getCriteria());
			case AND -> new Criteria().andOperator(left.getCriteria(), right.getCriteria());
			//fixme: исправить
			case NOT -> throw new UnsupportedOperationException("Оператор '%s' не поддерживается в MongoDictDataBackend.".formatted(expression.type));

			default -> throw new AppException(ApiStatusCodeImpl.ILLEGAL_INPUT, "Не поддерживаемый оператор '%s'".formatted(expression.type));
		};

		return MongoQuery.builder()
				.criteria(criteria)
				.participantDictIds(participantDictIds)
				.build();
	}

	@Override
	public MongoQuery visit(Predicate predicate, TranslationContext context)
	{
		var left = predicate.left.process(this, context).getField();
		var right = predicate.right.process(this, context);

		var criteria = Criteria.where(left.getFieldId());
		var rightValue = right.getValue();

		switch (predicate.type)
		{
			case EQ -> criteria.is(rightValue);
			case NEQ -> criteria.ne(rightValue);
			case LS -> criteria.lt(rightValue);
			case GT -> criteria.gt(rightValue);
			case LEQ -> criteria.lte(rightValue);
			case GEQ -> criteria.gte(rightValue);

			default -> throw new AppException(ApiStatusCodeImpl.ILLEGAL_INPUT, "Не поддерживаемый оператор '%s'".formatted(predicate.type));
		}

		return MongoQuery.builder()
				.criteria(criteria)
				.participantDictIds(participantDictIds(left))
				.build();
	}

	@Override
	public MongoQuery visit(InQueryExpression expression, TranslationContext context)
	{
		var left = expression.field.process(this, context).getField();
		var variants = expression.variants.stream()
				.map(it -> it.process(this, context))
				.map(MongoQuery::getValue)
				.toList();

		var criteria = Criteria.where(left.getFieldId())
				.in(variants);

		return MongoQuery.builder()
				.criteria(criteria)
				.participantDictIds(participantDictIds(left))
				.build();
	}

	@Override
	public MongoQuery visit(AllOrAnyQueryExpression expression, TranslationContext context)
	{
		var left = expression.field.process(this, context).getField();
		var constants = expression.constants.stream()
				.map(it -> it.process(this, context))
				.map(MongoQuery::getValue)
				.toList();

		var criteria = Criteria.where(left.getFieldId());

		switch (expression.type)
		{
			case ALL -> criteria.all(constants);
			case ANY -> criteria.in(constants);

			default -> throw new AppException(ApiStatusCodeImpl.ILLEGAL_INPUT, "Не поддерживаемый оператор '%s'".formatted(expression.type));
		}

		return MongoQuery.builder()
				.criteria(criteria)
				.participantDictIds(participantDictIds(left))
				.build();
	}

	@Override
	public MongoQuery visit(Empty empty, TranslationContext context)
	{
		return MongoQuery.builder().build();
	}

	private Set<String> participantDictIds(MongoQueryField field)
	{
		return field.getDictId() != null ? Set.of(field.getDictId()) : Collections.emptySet();
	}
}
