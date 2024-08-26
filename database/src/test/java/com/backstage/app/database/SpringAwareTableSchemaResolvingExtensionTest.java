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

package com.backstage.app.database;

import com.backstage.app.database.configuration.jpa.eclipselink.uuid.UuidSupportExtension;
import com.backstage.app.database.domain.Customer;
import com.backstage.app.database.domain.Product;
import com.backstage.app.database.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты на работоспособность динамической конфигурации схемы БД для Entity
 *
 * @see UuidSupportExtension
 */
public class SpringAwareTableSchemaResolvingExtensionTest extends AbstractTest
{
	@Autowired
	TicketRepository ticketRepository;

	@Autowired
	NamedParameterJdbcTemplate jdbcTemplate;

	@Test
	void selectEntityWithSpelSpecifiedScheme()
	{
		assertDoesNotThrow(() ->
				jdbcTemplate.queryForList("select * from test_scheme.customer", new MapSqlParameterSource(), Customer.class));
	}

	@Test
	void selectEntityWithPlaceholderSpecifiedScheme()
	{
		assertDoesNotThrow(() ->
				jdbcTemplate.queryForList("select * from test_scheme.product", new MapSqlParameterSource(), Product.class));
	}

	@Test
	void selectEntityWithMultiplyTableSpelSpecifiedScheme()
	{
		var tickets = ticketRepository.findAll();

		assertFalse(tickets.isEmpty());

		var ticket = tickets.get(0);

		assertNotNull(ticket.getTitle());
		assertNotNull(ticket.getDescription());
		assertNotNull(ticket.getSenderEmail());
	}
}
