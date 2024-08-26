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

package com.backstage.app.dict.service;

import com.backstage.app.dict.common.CommonTest;
import com.backstage.app.dict.exception.dict.query.QuerySyntaxException;
import com.backstage.app.dict.service.query.QueryParser;
import com.backstage.app.dict.service.query.ast.Constant;
import com.backstage.app.dict.service.query.ast.InQueryExpression;
import com.backstage.app.dict.service.query.ast.LogicQueryExpression;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

class QueryParserTest extends CommonTest
{
	@Autowired
	private QueryParser queryParser;

	@Test
	public void parse_timestampQueryTest()
	{
		var expression = queryParser.parse("timestampField in ('2020-03-01'::timestamp, '2020-03-01+03:00'::timestamp, '2020-04-01T10:22:00'::timestamp, '2020-04-01T10:22:00.555'::timestamp, '2020-04-01T10:22:00+03:00'::timestamp, '2020-04-01T10:22:00.555+03:00'::timestamp)");

		assertTrue(expression instanceof InQueryExpression);

		var expected = ((InQueryExpression) expression).variants.stream().filter(it -> it.type == Constant.Type.TIMESTAMP).count();
		var actual = ((InQueryExpression) expression).variants.size();

		assertEquals(expected, actual);
	}

	@Test
	public void parse_dateQueryTest()
	{
		var expression = queryParser.parse("timestampField in ('2020-03-01'::date, '2020-03-01+03:00'::date, '2020-04-01T10:22:00'::date, '2020-04-01T10:22:00.555'::date, '2020-04-01T10:22:00+03:00'::date, '2020-04-01T10:22:00.555+03:00'::date)");

		assertTrue(expression instanceof InQueryExpression);

		var expected = ((InQueryExpression) expression).variants.stream().filter(it -> it.type == Constant.Type.DATE).count();
		var actual = ((InQueryExpression) expression).variants.size();

		assertEquals(expected, actual);
	}

	@Test
	public void parse_logicTest()
	{
		var expression = queryParser.parse("field1 = 1 and (field2 = 2 or field3 = 3) and not (field4 = 4)");

		assertTrue(expression instanceof LogicQueryExpression);

		var expected = ((LogicQueryExpression) expression).type;
		var actual = LogicQueryExpression.Type.AND;

		assertSame(expected, actual);
	}

	@Test
	public void parse_invalidQuery()
	{
		assertThrows(QuerySyntaxException.class, () -> queryParser.parse("stringField = stringField"));
	}
}