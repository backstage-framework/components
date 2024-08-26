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

import com.backstage.app.dict.domain.Dict;
import com.backstage.app.dict.exception.BiFunctionException;
import com.backstage.app.dict.exception.TriFunctionException;
import com.backstage.app.dict.model.postgres.backend.PostgresWord;
import com.backstage.app.dict.service.migration.DictTransactionProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractPostgresBackend
{
	@Autowired
	protected ObjectMapper mapper;

	@Autowired
	protected NamedParameterJdbcTemplate jdbc;

	@Autowired
	protected PostgresEngine postgresEngine;

	@Autowired
	protected DictTransactionProvider dictTransactionProvider;

	@Autowired
	private PostgresReservedKeyword reservedKeywords;

	protected void addTransactionData(Dict dict, boolean schemeUsed)
	{
		dictTransactionProvider.addDictTransactionItem(dict, schemeUsed);
	}

	protected <F, E extends Throwable, T extends Throwable> void transactionWithoutResult(Runnable runnable, F dictId, BiFunctionException<F, E, T> exception)
	{
		try
		{
			runnable.run();
		}
		catch (Exception e)
		{
			throw new RuntimeException(exception.apply(dictId, (E) e));
		}
	}

	protected <F, S, E extends Throwable, T extends Throwable> void transactionWithoutResult(Runnable runnable, F dictId, S dictElementId, TriFunctionException<F, S, E, T> exception)
	{
		try
		{
			runnable.run();
		}
		catch (Exception e)
		{
			throw new RuntimeException(exception.apply(dictId, dictElementId, (E) e));
		}
	}

	protected <R, F, E extends Throwable, T extends Throwable> R transactionWithResult(Callable<R> callable, F dictId, BiFunctionException<F, E, T> exception)
	{
		try
		{
			return callable.call();
		}
		catch (Exception e)
		{
			throw new RuntimeException(exception.apply(dictId, (E) e));
		}
	}

	protected <R, F, S, E extends Throwable, T extends Throwable> R transactionWithResult(Callable<R> callable, F dictId, S dictElementId, TriFunctionException<F, S, E, T> exception)
	{
		try
		{
			return callable.call();
		}
		catch (Exception e)
		{
			throw new RuntimeException(exception.apply(dictId, dictElementId, (E) e));
		}
	}

	protected Map<String, PostgresWord> wordMap(String... words)
	{
		return reservedKeywords.postgresWordMap(words);
	}

	protected Map<String, PostgresWord> wordMap(List<String> wordList, String... words)
	{
		return Stream.concat(wordList.stream(), Arrays.stream(words))
				.map(reservedKeywords::postgresWordMap)
				.map(Map::entrySet)
				.flatMap(Collection::stream)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldV, newV) -> oldV));
	}

	protected String sqlWithParameters(String sql, Map<String, String> parameterMap)
	{
		return StringSubstitutor.replace(sql, parameterMap);
	}

	protected void addParameter(MapSqlParameterSource parameterMap, String parameterName, Object value)
	{
		parameterMap.addValue(parameterName, value);
	}
}
