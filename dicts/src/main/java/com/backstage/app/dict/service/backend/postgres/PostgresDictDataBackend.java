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

package com.backstage.app.dict.service.backend.postgres;

import com.backstage.app.dict.configuration.conditional.ConditionalOnEngine;
import com.backstage.app.dict.configuration.properties.DictsProperties;
import com.backstage.app.dict.domain.Dict;
import com.backstage.app.dict.domain.DictFieldName;
import com.backstage.app.dict.domain.DictItem;
import com.backstage.app.dict.exception.dictitem.DictItemCreatedException;
import com.backstage.app.dict.exception.dictitem.DictItemDeletedException;
import com.backstage.app.dict.exception.dictitem.DictItemUpdatedException;
import com.backstage.app.dict.model.dictitem.DictItemColumnName;
import com.backstage.app.dict.model.postgres.backend.PostgresDictFieldName;
import com.backstage.app.dict.model.postgres.backend.PostgresDictItem;
import com.backstage.app.dict.model.postgres.backend.PostgresPageable;
import com.backstage.app.dict.model.postgres.query.PostgresQuery;
import com.backstage.app.dict.service.DictService;
import com.backstage.app.dict.service.backend.DictDataBackend;
import com.backstage.app.dict.service.backend.Engine;
import com.backstage.app.dict.service.backend.postgres.clause.PostgresDictDataInsertClause;
import com.backstage.app.dict.service.backend.postgres.clause.PostgresDictDataQueryClause;
import com.backstage.app.dict.service.backend.postgres.clause.PostgresDictDataUpdateClause;
import com.backstage.app.dict.service.query.PostgresTranslator;
import com.backstage.app.dict.service.query.QueryParser;
import com.backstage.app.dict.service.query.ast.QueryExpression;
import com.backstage.app.exception.ObjectNotFoundException;
import com.backstage.app.utils.DataUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@ConditionalOnEngine(PostgresEngine.POSTGRES)
public class PostgresDictDataBackend extends AbstractPostgresBackend implements DictDataBackend
{
	private final DictService dictService;

	private final QueryParser queryParser;
	private final PostgresTranslator postgresTranslator;

	private final PostgresDictDataInsertClause insertClause;
	private final PostgresDictDataUpdateClause updateClause;
	private final PostgresDictDataQueryClause filterClause;

	private final PostgresPageableMapper pageableMapper;
	private final PostgresDictFieldNameMapper fieldNameMapper;
	private final PostgresDictDataBackendMapper dataBackendMapper;

	private final DictsProperties dictsProperties;

	@Override
	public Engine getEngine()
	{
		return postgresEngine;
	}

	@Override
	public DictItem getById(Dict dict, String id, List<DictFieldName> requiredFields)
	{
		var query = "%s = '%s'".formatted(DictItemColumnName.ID.getName(), id);

		return getByFilter(dict, requiredFields, queryParser.parse(query), Pageable.unpaged())
				.stream()
				.findFirst()
				.orElseThrow(() -> new ObjectNotFoundException(DictItem.class, "dictId: %s, itemId: %s".formatted(dict.getId(), id)));
	}

	@Override
	public List<DictItem> getByIds(Dict dict, List<String> ids, List<DictFieldName> requiredFields)
	{
		var itemIds = ids.stream()
				.map(it -> "'" + it + "'")
				.collect(Collectors.joining(", "));

		var filtersQuery = "%s in (%s)".formatted(DictItemColumnName.ID.getName(), itemIds);

		return getByFilter(dict, requiredFields, queryParser.parse(filtersQuery), Pageable.unpaged()).getContent();
	}

	@Override
	public List<Object> getDistinctValuesByFilter(Dict dict, DictFieldName requiredField, QueryExpression queryExpression)
	{
		var dictId = dict.getId();

		var dictAliasesRelation = new DualHashBidiMap<String, String>();
		var joinClauses = new LinkedHashSet<String>();
		var whereClauses = new LinkedHashSet<String>();

		var query = postgresTranslator.process(dict, queryExpression);

		var postgresDictFieldName = fieldNameMapper.mapFrom(dictId, requiredField);

		// selectClauses и orderByClauses не используются, но передается для возможности
		// переиспользования существующего кода
		completeFilterClauses(dictAliasesRelation, new LinkedHashSet<>(), joinClauses, whereClauses, new LinkedHashSet<>(), query,
				List.of(postgresDictFieldName), PostgresPageable.UNPAGED, dict, dictService);

		var wordDictId = wordMap(dictId)
				.get(dictId)
				.getQuotedIfKeyword();
		String dictFieldJoint = postgresDictFieldName.getWordJoint();

		//TODO: провести рефакторинг билда sql
		var sqlIds = "select distinct %s as value".formatted(dictFieldJoint)
				+ " from %s.".formatted(dictsProperties.getDdl().getScheme()) + wordDictId + (joinClauses.isEmpty() ? "" : " " + String.join(" ", joinClauses))
				+ (whereClauses.isEmpty() ? "" : " where " + String.join(" and ", whereClauses))
				+ " order by %s asc".formatted(dictFieldJoint);

		return jdbc.queryForList(sqlIds, query.getParameterSource())
				.stream()
				.map(it -> it.get("value"))
				.toList();
	}

	@Override
	public Page<DictItem> getByFilter(Dict dict, List<DictFieldName> requiredFields, QueryExpression queryExpression, Pageable pageable)
	{
		var dictId = dict.getId();

		var dictAliasesRelation = new DualHashBidiMap<String, String>();
		var selectClauses = new LinkedHashSet<String>();
		var joinClauses = new LinkedHashSet<String>();
		var whereClauses = new LinkedHashSet<String>();
		var orderByClauses = new LinkedHashSet<String>();
		var postgresPageable = pageableMapper.mapFrom(dictId, pageable);

		var query = postgresTranslator.process(dict, queryExpression);

		completeFilterClauses(dictAliasesRelation, selectClauses, joinClauses, whereClauses, orderByClauses, query,
				fieldNameMapper.mapFrom(dictId, requiredFields), postgresPageable, dict, dictService);

		var wordDictId = wordMap(dictId).get(dictId).getQuotedIfKeyword();

		//TODO: провести рефакторинг билда sql
		var sqlIds = "select " + (joinClauses.isEmpty() ? "" : "distinct ") + String.join(", ", selectClauses)
				+ " from %s.".formatted(dictsProperties.getDdl().getScheme()) + wordDictId + (joinClauses.isEmpty() ? "" : " " + String.join(" ", joinClauses))
				+ (whereClauses.isEmpty() ? "" : " where " + String.join(" and ", whereClauses))
				+ (orderByClauses.isEmpty() ? "" : " order by " + String.join(", ", orderByClauses))
				+ (postgresPageable.isPaged() ? " limit " + postgresPageable.getPageSize() + " offset " + postgresPageable.getOffset() : "");

		var countSql = "select count(" + (joinClauses.isEmpty() ? "" : "distinct ") + wordDictId + ".id) from %s.".formatted(dictsProperties.getDdl().getScheme()) + wordDictId
				+ (joinClauses.isEmpty() ? "" : " " + String.join(" ", joinClauses))
				+ (whereClauses.isEmpty() ? "" : " where " + String.join(" and ", whereClauses));

		var postgresItems = jdbc.queryForList(sqlIds, query.getParameterSource())
				.stream()
				.map(it -> new PostgresDictItem(dictId, it, dictAliasesRelation.get(dictId.toLowerCase())))
				.toList();

		var dictItems = dataBackendMapper.mapFromUsingAliases(dictId, postgresItems, dictAliasesRelation);

		if (dictItems.isEmpty())
		{
			return DataUtils.emptyPage(pageable);
		}

		var count = Objects.requireNonNull(jdbc.queryForObject(countSql, query.getParameterSource(), Long.class));

		return new PageImpl<>(dictItems, pageable, count);
	}

	@Override
	public boolean existsById(Dict dict, String itemId)
	{
		var dictId = dict.getId();

		var parameterMap = Map.of(
				"scheme", dictsProperties.getDdl().getScheme(),
				"dictId", wordMap(dictId).get(dictId).getQuotedIfKeyword().toLowerCase(),
				"itemId", itemId
		);

		var sql = sqlWithParameters("select exists(select 1 from ${scheme}.${dictId} where id = '${itemId}')", parameterMap);

		return Boolean.TRUE.equals(jdbc.queryForObject(sql, parameterMap, Boolean.class));
	}

	@Override
	public boolean existsByFilter(Dict dict, QueryExpression queryExpression)
	{
		return getByFilter(dict, List.of(new DictFieldName(null, "*")), queryExpression, Pageable.unpaged())
				.getContent()
				.stream()
				.findFirst()
				.isPresent();
	}

	@Override
	public DictItem create(Dict dict, DictItem dictItem)
	{
		addTransactionData(dict, false);

		return transactionWithResult(() -> createItem(dict, dictItem), dict.getId(), DictItemCreatedException::new);
	}

	@Override
	public List<DictItem> createMany(Dict dict, List<DictItem> dictItems)
	{
		addTransactionData(dict, false);

		var dictId = dict.getId();

		return transactionWithResult(() -> createItems(dict, dictItems), dictId, DictItemCreatedException::new);
	}

	@Override
	public DictItem update(Dict dict, String itemId, DictItem dictItem, long version)
	{
		addTransactionData(dict, false);

		var dictId = dict.getId();

		return transactionWithResult(() -> updateItem(dict, itemId, dictItem), dictId, itemId, DictItemUpdatedException::new);
	}

	@Override
	public void delete(Dict dict, DictItem dictItem)
	{
		addTransactionData(dict, false);

		transactionWithoutResult(() -> deleteItem(dict, dictItem), dict.getId(), dictItem.getId(), DictItemDeletedException::new);
	}

	@Override
	public void deleteAll(Dict dict, List<DictItem> dictItems)
	{
		addTransactionData(dict, false);

		transactionWithoutResult(() -> deleteItems(dict, dictItems), dict.getId(), String.join(", ", dictItems.stream().map(DictItem::getId).toList()), DictItemDeletedException::new);
	}

	@Override
	public long countByFilter(Dict dict, QueryExpression queryExpression)
	{
		return getByFilter(dict, List.of(new DictFieldName(null, "*")), queryExpression, Pageable.unpaged())
				.getTotalElements();
	}

	private DictItem createItem(Dict dict, DictItem dictItem)
	{
		var columns = new LinkedHashSet<String>();

		dictItem.setId(dictItem.getId() == null ? String.valueOf(UUID.randomUUID()) : dictItem.getId());

		var dictId = dict.getId();
		var sqlParameterSource = new MapSqlParameterSource();

		completeInsertClauses(columns, dict, dataBackendMapper.mapTo(dictId, dictItem), sqlParameterSource);

		var parameterMap = Map.of(
				"scheme", dictsProperties.getDdl().getScheme(),
				"dictId", wordMap(dictId).get(dictId).getQuotedIfKeyword(),
				"columns", String.join(", ", columns),
				"values", Arrays.stream(sqlParameterSource.getParameterNames()).collect(Collectors.joining(", :", ":", ""))
		);

		var sql = sqlWithParameters("insert into ${scheme}.${dictId} (${columns}) values (${values})", parameterMap);

		jdbc.update(sql, sqlParameterSource);

		return getById(dict, dictItem.getId(), List.of(new DictFieldName(null, "*")));
	}

	private List<DictItem> createItems(Dict dict, List<DictItem> dictItems)
	{
		return dictItems.stream()
				.map(dictItem -> createItem(dict, dictItem))
				.collect(Collectors.toList());
	}

	private DictItem updateItem(Dict dict, String itemId, DictItem dictItem)
	{
		var updateClauses = new LinkedHashSet<String>();

		var dictId = dict.getId();
		var sqlParameterSource = new MapSqlParameterSource();

		completeUpdateClause(updateClauses, dict, itemId, dataBackendMapper.mapTo(dictId, dictItem), sqlParameterSource);

		var parameterMap = Map.of(
				"scheme", dictsProperties.getDdl().getScheme(),
				"dictId", wordMap(dictId).get(dictId).getQuotedIfKeyword(),
				"updateClauses", String.join(", ", updateClauses),
				"itemId", itemId
		);

		var sql = sqlWithParameters("update ${scheme}.${dictId} set ${updateClauses} where id = '${itemId}'", parameterMap);

		jdbc.update(sql, sqlParameterSource);

		return getById(dict, itemId, List.of(new DictFieldName(null, "*")));
	}

	private void deleteItem(Dict dict, DictItem dictItem)
	{
		var updateClauses = new LinkedHashSet<String>();

		var dictId = dict.getId();
		var sqlParameterSource = new MapSqlParameterSource();

		completeUpdateClause(updateClauses, dict, dictItem.getId(), dataBackendMapper.mapTo(dictId, dictItem), sqlParameterSource);

		var parameterMap = Map.of(
				"scheme", dictsProperties.getDdl().getScheme(),
				"dictId", wordMap(dictId).get(dictId).getQuotedIfKeyword(),
				"updateClauses", String.join(", ", updateClauses),
				"itemId", dictItem.getId()
		);

		var sql = sqlWithParameters("update ${scheme}.${dictId} set ${updateClauses} where id = '${itemId}'", parameterMap);

		jdbc.update(sql, sqlParameterSource);
	}

	private void deleteItems(Dict dict, List<DictItem> dictItems)
	{
		dictItems.forEach(dictItem -> deleteItem(dict, dictItem));
	}

	private void completeInsertClauses(LinkedHashSet<String> columns, Dict dict, PostgresDictItem postgresDictItem, MapSqlParameterSource sqlParameterSource)
	{
		insertClause.addInsertClause(DictItemColumnName.ID.getName(), postgresDictItem.getId(), columns, sqlParameterSource);
		insertClause.addDictDataInsertClause(dict, postgresDictItem, columns, sqlParameterSource);
		insertClause.addInsertJsonClause(DictItemColumnName.HISTORY.getName(), postgresDictItem.getHistory(), columns, sqlParameterSource);
		insertClause.addInsertClause(DictItemColumnName.VERSION.getName(), postgresDictItem.getVersion(), columns, sqlParameterSource);
		insertClause.addInsertClause(DictItemColumnName.CREATED.getName(), postgresDictItem.getCreated(), columns, sqlParameterSource);
		insertClause.addInsertClause(DictItemColumnName.UPDATED.getName(), postgresDictItem.getUpdated(), columns, sqlParameterSource);
	}

	private void completeUpdateClause(LinkedHashSet<String> updateClauses, Dict dict, String itemId, PostgresDictItem postgresDictItem, MapSqlParameterSource sqlParameterSource)
	{
		var oldItem = getById(dict, itemId, List.of(new DictFieldName(null, "*")));

		updateClause.addDictDataUpdateClause(dict, oldItem.getData(), postgresDictItem.getDictData(), updateClauses, sqlParameterSource);
		updateClause.addUpdateClause(DictItemColumnName.VERSION.getName(), oldItem.getVersion(), postgresDictItem.getVersion(), updateClauses, sqlParameterSource);
		updateClause.addUpdateJsonClause(DictItemColumnName.HISTORY.getName(), oldItem.getHistory(), postgresDictItem.getHistory(), updateClauses, sqlParameterSource);
		updateClause.addUpdateClause(DictItemColumnName.UPDATED.getName(), oldItem.getUpdated(), postgresDictItem.getUpdated(), updateClauses, sqlParameterSource);
		updateClause.addUpdateClause(DictItemColumnName.DELETED.getName(), oldItem.getDeleted(), postgresDictItem.getDeleted(), updateClauses, sqlParameterSource);
		updateClause.addUpdateClause(DictItemColumnName.DELETION_REASON.getName(), oldItem.getDeletionReason(), postgresDictItem.getDeletionReason(), updateClauses, sqlParameterSource);
	}

	private void completeFilterClauses(BidiMap<String, String> dictAliasesRelation,
	                                   LinkedHashSet<String> selectClauses, LinkedHashSet<String> joinClauses,
	                                   LinkedHashSet<String> whereClauses, LinkedHashSet<String> orderByClauses,
	                                   PostgresQuery queryContext, List<PostgresDictFieldName> requiredFields,
	                                   PostgresPageable postgresPageable, Dict dict, DictService dictService)
	{
		filterClause.addSelectClauses(dictAliasesRelation, selectClauses, requiredFields, queryContext, postgresPageable, dict, dictService);
		filterClause.addJoinClauses(joinClauses, requiredFields, queryContext, postgresPageable, dict);
		filterClause.addWhereClauses(whereClauses, queryContext);
		filterClause.addOrderByClauses(orderByClauses, postgresPageable, dict);
	}
}
