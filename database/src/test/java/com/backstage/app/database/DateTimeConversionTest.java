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

import com.backstage.app.database.domain.Ticket;
import com.backstage.app.database.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Тесты проверяют работоспособность конвертации поддерживаемых типов даты-времени.
 */
public class DateTimeConversionTest extends AbstractTest
{
	@Autowired
	TicketRepository ticketRepository;

	@Test
	void selectExistsDateTimeEntity()
	{
		assertDoesNotThrow(() -> ticketRepository.findAll());
	}

	@Test
	void insertDateTimeEntity()
	{
		var ticketCreated = LocalDateTime.of(2021, 8, 15, 6, 0, 0);

		var sourceTicket = new Ticket();
		sourceTicket.setCreated(ticketCreated);
		sourceTicket.setSenderEmail("email");

		var ticket = ticketRepository.save(sourceTicket);

		assertEquals(ticketCreated, ticket.getCreated());
	}
}
