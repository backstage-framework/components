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

package com.backstage.app.jms.service;

import com.backstage.app.jms.AbstractTests;
import com.backstage.app.utils.TimeUtils;
import jakarta.jms.JMSException;
import jakarta.jms.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class JmsTransactionTests extends AbstractTests
{
	public static final String JMS_TEST_CHANNEL_TX = "testChannelTx";
	public static final String JMS_TEST_CHANNEL_NO_TX = "testChannelNoTx";

	private final int JMS_TIMEOUT_SECONDS = 1;

	@Autowired private TransactionTemplate transactionTemplate;
	@Autowired private JmsTemplate jmsTemplate;

	static String testValueTx;
	static String testValueNoTx;

	@BeforeEach
	public void beforeTest()
	{
		testValueTx = null;
		testValueNoTx = null;
	}

	@Test
	public void sendInTransaction()
	{
		var valueTx = UUID.randomUUID().toString();
		var valueNoTx = UUID.randomUUID().toString();

		transactionTemplate.executeWithoutResult((status) -> {
			jmsTemplate.convertAndSend(JMS_TEST_CHANNEL_TX, valueTx);
			jmsTemplate.convertAndSend(JMS_TEST_CHANNEL_NO_TX, valueNoTx);

			TimeUtils.sleepSeconds(JMS_TIMEOUT_SECONDS);
		});

		TimeUtils.sleepSeconds(JMS_TIMEOUT_SECONDS);

		assertEquals(valueTx, testValueTx);
		assertEquals(valueNoTx, testValueNoTx);
	}

	@Test
	public void sendNoTransaction()
	{
		var valueTx = UUID.randomUUID().toString();
		var valueNoTx = UUID.randomUUID().toString();

		jmsTemplate.convertAndSend(JMS_TEST_CHANNEL_TX, valueTx);
		jmsTemplate.convertAndSend(JMS_TEST_CHANNEL_NO_TX, valueNoTx);

		TimeUtils.sleepSeconds(JMS_TIMEOUT_SECONDS);

		assertEquals(valueTx, testValueTx);
		assertEquals(valueNoTx, testValueNoTx);
	}

	@JmsListener(destination = JMS_TEST_CHANNEL_TX)
	public void handleJmsMessageTx(Session session, String value) throws JMSException
	{
		if (session.getTransacted())
		{
			testValueTx = value;
		}
	}

	@JmsListener(destination = JMS_TEST_CHANNEL_NO_TX, containerFactory = "nonTxJmsListenerContainerFactory")
	public void handleJmsMessageNoTx(Session session, String value) throws JMSException
	{
		if (!session.getTransacted())
		{
			testValueNoTx = value;
		}
	}
}
