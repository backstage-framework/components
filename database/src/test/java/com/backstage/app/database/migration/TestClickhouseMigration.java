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

package com.backstage.app.database.migration;

import com.backstage.app.database.AbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TestClickhouseMigration extends AbstractTest
{
	@Autowired
	@Qualifier("clickHouseJdbcTemplate")
	NamedParameterJdbcTemplate clickhouse;

	@Test
	void checkIfTestTableExistsTest()
	{
		var result = clickhouse.queryForObject("exists table test", Map.of(), Integer.class);

		assertEquals(1, result);
	}

	@Test
	void checkIfRecordsInTestTableExistsTest()
	{
		var result = clickhouse.queryForList("select * from test", Map.of());

        assertFalse(result.isEmpty());
	}
}
