/*
 *    Copyright 2019-2026 the original author or authors.
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
import com.backstage.app.audit.repository.AuditRepository;
import com.backstage.app.utils.TimeUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class JmsAuditStoreTest extends AbstractTests
{
	@Autowired
	@Qualifier("jmsAuditWriter")
	private AuditStore auditStore;

	@Autowired
	private AuditRepository auditRepository;

	@AfterAll
	void tearDown()
	{
		auditRepository.deleteAll();
	}

	@Test
	void log_AuditEventAsync()
	{
		var objectId = UUID.randomUUID().toString();

		auditStore.write(AuditEventBuilder.create(AuditServiceTests.TestEventTypes.EVENT_TYPE_4, objectId).build());

		TimeUtils.sleepSeconds(1);

		var auditEvents = auditRepository.findAllByTypeAndObjectId(AuditServiceTests.TestEventTypes.EVENT_TYPE_4.name(), objectId);

		assertEquals(1, auditEvents.size());
		assertEquals(objectId, auditEvents.getFirst().getObjectId());
	}
}
