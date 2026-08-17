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

package com.backstage.app.audit.service;

import com.backstage.app.audit.AbstractTests;
import com.backstage.app.audit.model.other.AuditEventBuilder;
import com.backstage.app.audit.model.other.AuditFilter;
import com.backstage.app.audit.repository.AuditRepository;
import com.backstage.app.utils.TimeUtils;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Pageable;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JpaAuditStoreTest extends AbstractTests
{
	private static final int GET_BY_FILTER_TEST_ORDER = -100;

	@Autowired
	@Qualifier("jpaAuditWriter")
	private AuditStore auditStore;

	@Autowired
	private AuditRepository auditRepository;

	@BeforeAll
	void initTestData()
	{
		Stream.of(AuditEventBuilder.create(AuditServiceTests.TestEventTypes.EVENT_TYPE_3, AuditServiceTests.TestData.USER_2_ID),
						AuditEventBuilder.create(AuditServiceTests.TestEventTypes.EVENT_TYPE_3, AuditServiceTests.TestData.USER_2_ID, AuditServiceTests.TestData.USER_1_ID),
						AuditEventBuilder.create(AuditServiceTests.TestEventTypes.EVENT_TYPE_3, AuditServiceTests.TestData.USER_1_ID, AuditServiceTests.TestData.USER_2_ID),
						AuditEventBuilder.create(AuditServiceTests.TestEventTypes.EVENT_TYPE_2, AuditServiceTests.TestData.USER_1_ID, AuditServiceTests.TestData.ADMIN_ID),
						AuditEventBuilder.create(AuditServiceTests.TestEventTypes.EVENT_TYPE_1, AuditServiceTests.TestData.USER_1_ID, AuditServiceTests.TestData.ADMIN_ID),
						AuditEventBuilder.create(AuditServiceTests.TestData.TYPE_1, AuditServiceTests.TestData.ADMIN_ID),
						AuditEventBuilder.create(AuditServiceTests.TestData.TYPE_2, AuditServiceTests.TestData.ADMIN_ID, AuditServiceTests.TestData.ADMIN_ID))
				.map(AuditEventBuilder::build)
				.forEach(auditStore::write);

		TimeUtils.sleepSeconds(2);
	}

	@AfterAll
	void tearDown()
	{
		auditRepository.deleteAll();
	}

	@Test
	@Order(GET_BY_FILTER_TEST_ORDER)
	void getByFilter()
	{
		var actual = auditStore.getByFilter(AuditFilter.builder().build(), Pageable.unpaged());

		assertEquals(7, actual.getTotalElements());
	}

	@Test
	void log_AuditEventNullProperty()
	{
		assertDoesNotThrow(() -> auditStore.write(AuditEventBuilder.create(AuditServiceTests.TestEventTypes.EVENT_TYPE_4, AuditServiceTests.TestData.USER_1_ID)
				.withProperty(AuditServiceTests.TestData.TYPE_4_PROPERTY_KEY, null)
				.build()));
	}

	@Test
	void log_AuditEventNullField()
	{
		assertDoesNotThrow(() -> auditStore.write(AuditEventBuilder.create(AuditServiceTests.TestEventTypes.EVENT_TYPE_4, AuditServiceTests.TestData.USER_1_ID)
				.withField(AuditServiceTests.TestData.TYPE_4_FIELD_KEY, AuditServiceTests.TestData.TYPE_4_FIELD_OLD_VALUE, null)
				.build()));
	}
}