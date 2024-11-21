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

import com.backstage.app.dict.configuration.properties.DictsProperties;
import com.backstage.app.dict.constant.ServiceFieldConstants;
import com.backstage.app.dict.domain.Dict;
import com.backstage.app.dict.domain.DictField;
import com.backstage.app.dict.model.postgres.backend.PostgresDictFieldName;
import com.backstage.app.dict.model.postgres.backend.PostgresOrder;
import com.backstage.app.dict.model.postgres.backend.PostgresPageable;
import com.backstage.app.dict.model.postgres.backend.PostgresWord;
import com.backstage.app.dict.model.postgres.query.PostgresQuery;
import com.backstage.app.dict.service.DictService;
import com.backstage.app.dict.service.backend.postgres.PostgresDictFieldNameMapper;
import com.backstage.app.utils.ValidationUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.BidiMap;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PostgresDictDataQueryClause
{
	private static final String DICT_ALIAS_KEY = "t";
	private static final String SELECT_CLAUSE = "%s.%s as \"%s__%s\"";
	private static final String JOIN_CLAUSE = "left join %s.%s on %s.%s.%s = %s.%s";
	private static final String ASCENDING_ID_ORDER_CLAUSE = "%s asc";

	private final PostgresDictFieldNameMapper dictFieldNameMapper;

	private final DictsProperties dictsProperties;

	public void addSelectClauses(BidiMap<String, String> dictAliasesRelation, HashSet<String> selectClauses, List<PostgresDictFieldName> requiredFields,
	                             PostgresQuery queryContext, PostgresPageable postgresPageable, Dict dict, DictService dictService)
	{
		addSelectIdClause(dictAliasesRelation, selectClauses, requiredFields, queryContext, postgresPageable, dict);

		var isAllFields = requiredFields.stream()
				.map(PostgresDictFieldName::getWordFieldId)
				.map(PostgresWord::getOriginalWord)
				.allMatch("*"::equals);

		if (isAllFields)
		{
			requiredFields.stream()
					.map(field -> dictService.getById(field.getWordDictId().getOriginalWord())
							.getFields()
							.stream()
							.map(DictField::getId)
							.map(it -> selectClause(dictAliasesRelation, field, it))
							.collect(Collectors.toCollection(LinkedHashSet::new)))
					.forEach(selectClauses::addAll);

			return;
		}

		requiredFields.stream()
				.filter(it -> "*".equals(it.getWordFieldId().getOriginalWord()))
				.map(field -> dictService.getById(field.getWordDictId().getOriginalWord())
						.getFields()
						.stream()
						.map(DictField::getId)
						.map(it -> selectClause(dictAliasesRelation, field, it))
						.collect(Collectors.toCollection(LinkedHashSet::new)))
				.forEach(selectClauses::addAll);

		selectClauses.addAll(
				requiredFields.stream()
						.filter(it -> !"*".equals(it.getWordFieldId().getOriginalWord()))
						.map(it -> selectClause(dictAliasesRelation, it))
						.collect(Collectors.toCollection(LinkedHashSet::new))
		);

		if (postgresPageable != null && postgresPageable.isPaged())
		{
			selectClauses.addAll(
					postgresPageable.getPostgresSort()
							.getPostgresOrders()
							.stream()
							.map(PostgresOrder::getPostgresDictFieldName)
							.map(PostgresDictFieldName::getWordJoint)
							.collect(Collectors.toCollection(LinkedHashSet::new))
			);
		}
	}

	public void addJoinClauses(HashSet<String> joinClauses, List<PostgresDictFieldName> requiredFields,
	                           PostgresQuery queryContext, PostgresPageable postgresPageable, Dict dict)
	{
		var dictRefMap = DictService.getReferenceFieldMap(dict);

		if (dictRefMap.isEmpty())
		{
			return;
		}

		var dictId = dict.getId();

		requiredFields.stream()
				.map(PostgresDictFieldName::getWordDictId)
				.map(PostgresWord::getOriginalWord)
				.filter(dictRefMap::containsKey)
				.map(it -> joinClause(dictId, dictRefMap.get(it).getId(),
						dictRefMap.get(it).getDictRef().getDictId(), dictRefMap.get(it).getDictRef().getFieldId()))
				.forEach(joinClauses::add);

		if (postgresPageable != null && postgresPageable.isPaged())
		{
			postgresPageable.getPostgresSort()
					.getPostgresOrders()
					.stream()
					.map(PostgresOrder::getPostgresDictFieldName)
					.map(PostgresDictFieldName::getWordDictId)
					.map(PostgresWord::getOriginalWord)
					.filter(dictRefMap::containsKey)
					.map(it -> joinClause(dictId, dictRefMap.get(it).getId(),
							dictRefMap.get(it).getDictRef().getDictId(), dictRefMap.get(it).getDictRef().getFieldId()))
					.forEach(joinClauses::add);
		}

		//TODO: провалидировать refDictId's указанные в queryContext на наличие в Dict
		queryContext.getParticipantDictIds()
				.stream()
				.map(PostgresWord::getOriginalWord)
				.filter(dictRefMap::containsKey)
				.map(it -> joinClause(dictId, dictRefMap.get(it).getId(),
						dictRefMap.get(it).getDictRef().getDictId(), dictRefMap.get(it).getDictRef().getFieldId()))
				.forEach(joinClauses::add);
	}

	public void addWhereClauses(HashSet<String> whereClauses, PostgresQuery queryContext)
	{
		if (queryContext.getSqlExpression() != null)
		{
			whereClauses.add(queryContext.getSqlExpression());
		}
	}

	public void addOrderByClauses(LinkedHashSet<String> orderByClauses, PostgresPageable postgresPageable, Dict dict)
	{
		if (postgresPageable != null && postgresPageable.isPaged())
		{
			var ordersIncludeId = postgresPageable.getPostgresSort()
					.getPostgresOrders()
					.stream()
					.anyMatch(it -> ServiceFieldConstants.ID.equals(it.getPostgresDictFieldName().getWordJoint()));

			//TODO: провалидировать field's указанные в sort на наличие в dict/refDict
			var orders = postgresPageable.getPostgresSort()
					.getPostgresOrders()
					.stream()
					.map(it -> String.join(" ", it.getPostgresDictFieldName().getWordJoint(), it.getDirection().name()))
					.toList();

			orderByClauses.addAll(orders);

			if (!ordersIncludeId)
			{
				orderByClauses.add(ASCENDING_ID_ORDER_CLAUSE.formatted(dictFieldNameMapper.mapFrom(dict.getId(), ServiceFieldConstants.ID).getWordJoint()));
			}
		}
	}

	private void addSelectIdClause(BidiMap<String, String> dictAliasesRelation, HashSet<String> selectClauses, List<PostgresDictFieldName> requiredFields,
	                               PostgresQuery queryContext, PostgresPageable postgresPageable, Dict dict)
	{
		if (DictService.getReferenceFieldMap(dict).isEmpty())
		{
			return;
		}

		var dictId = dict.getId();

		requiredFields.stream()
				.filter(it -> !dictId.equals(it.getWordDictId().getOriginalWord()))
				.map(it -> selectClause(dictAliasesRelation, it, "id"))
				.forEach(selectClauses::add);

		queryContext.getParticipantDictIds()
				.stream()
				.filter(it -> !dictId.equals(it.getOriginalWord()))
				.map(it -> selectClause(dictAliasesRelation, it.getQuotedIfKeyword(), it.getOriginalWord(), "id"))
				.forEach(selectClauses::add);

		postgresPageable.getPostgresSort()
				.getPostgresOrders()
				.stream()
				.map(PostgresOrder::getPostgresDictFieldName)
				.filter(it -> !dictId.equals(it.getWordDictId().getOriginalWord()))
				.map(it -> selectClause(dictAliasesRelation, it, "id"))
				.forEach(selectClauses::add);
	}

	private String selectClause(BidiMap<String, String> tableAliasesRelation, PostgresDictFieldName fieldName)
	{
		return selectClause(tableAliasesRelation,
				fieldName.getWordDictId().getQuotedIfKeyword(),
				fieldName.getWordDictId().getOriginalWord(),
				fieldName.getWordFieldId().getOriginalWord());
	}

	private String selectClause(BidiMap<String, String> tableAliasesRelation, PostgresDictFieldName fieldName, String dictFieldId)
	{
		return selectClause(tableAliasesRelation,
				fieldName.getWordDictId().getQuotedIfKeyword(),
				fieldName.getWordDictId().getOriginalWord(),
				dictFieldId);
	}

	private String selectClause(BidiMap<String, String> dictAliasesRelation, String maybeQuotedDictId, String originalDictId, String originalFieldId)
	{
		var dictAlias = dictAliasesRelation.computeIfAbsent(originalDictId.toLowerCase(), k -> {
			var tableAliasesRelationIncrementedSize = dictAliasesRelation.size() + 1;

			return DICT_ALIAS_KEY + tableAliasesRelationIncrementedSize;
		});

		return SELECT_CLAUSE.formatted(maybeQuotedDictId, escapeName(originalFieldId), dictAlias, originalFieldId.toLowerCase());
	}

	private String joinClause(String dictId, String fieldId, String refDictId, String refFieldId)
	{
		var scheme = dictsProperties.getDdl().getScheme();

		return JOIN_CLAUSE.formatted(
				scheme,
				refDictId,
				scheme,
				dictId,
				escapeName(fieldId),
				refDictId,
				escapeName(refFieldId));
	}

	private String escapeName(String field)
	{
		return ValidationUtils.hasFirstDigit(field) ? "\"" + field + "\"" : field;
	}
}
