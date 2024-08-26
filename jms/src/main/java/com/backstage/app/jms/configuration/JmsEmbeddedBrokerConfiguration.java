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

package com.backstage.app.jms.configuration;

import com.backstage.app.jms.configuration.conditional.ConditionalOnEmbeddedBroker;
import com.backstage.app.jms.configuration.discovery.NoOpDiscoveryAgent;
import com.backstage.app.jms.configuration.properties.JmsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.broker.BrokerPlugin;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.region.policy.*;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.apache.activemq.network.DiscoveryNetworkConnector;
import org.apache.activemq.plugin.StatisticsBrokerPlugin;
import org.apache.activemq.store.PersistenceAdapter;
import org.apache.activemq.store.kahadb.KahaDBPersistenceAdapter;
import org.apache.activemq.store.memory.MemoryPersistenceAdapter;
import org.apache.activemq.transport.discovery.DiscoveryAgent;
import org.apache.activemq.util.DefaultIOExceptionHandler;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;

import java.io.File;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Configuration
@ConditionalOnEmbeddedBroker
@RequiredArgsConstructor
public class JmsEmbeddedBrokerConfiguration
{
	public static final String DLQ_PREFIX = "DLQ.";

	private final JmsProperties jmsProperties;

	@Bean
	public BrokerService brokerService(Optional<DiscoveryAgent> discoveryAgent, List<JmsBrokerChannelProvider> channelProviders) throws Exception
	{
		if (jmsProperties.isForceReleaseLock())
		{
			var lockFile = new File(jmsProperties.getStorePath(), "lock");

			if (lockFile.exists())
			{
				log.info("Force releasing JMS store lock file: {}.", lockFile);

				if (!lockFile.delete())
				{
					throw new RuntimeException("Failed to delete JMS store lock file!");
				}
			}
		}

		if (StringUtils.isBlank(jmsProperties.getBrokerName()))
		{
			jmsProperties.setBrokerName(String.format("app-instance-%s", UUID.randomUUID()));

			log.warn("Jms broker name is not specified. Using auto generated name: {}.", jmsProperties.getBrokerName());
		}

		var ioExceptionHandler = new DefaultIOExceptionHandler();
		ioExceptionHandler.setIgnoreNoSpaceErrors(false);
		ioExceptionHandler.setIgnoreSQLExceptions(false);

		BrokerService brokerService = new BrokerService();
		brokerService.addConnector(jmsProperties.getBrokerUrl());
		brokerService.setDestinationPolicy(setupDestinationPolicies());
		brokerService.setPersistent(true);
		brokerService.setPersistenceAdapter(persistenceAdapter());
		brokerService.setBrokerName(jmsProperties.getBrokerName());
		brokerService.setPlugins(new BrokerPlugin[] {
				new StatisticsBrokerPlugin()
		});
		brokerService.setIoExceptionHandler(ioExceptionHandler);

		if (jmsProperties.getStoreSizeLimit() != null)
		{
			brokerService.getSystemUsage().getStoreUsage().setLimit(DataSize.parse(jmsProperties.getStoreSizeLimit()).toBytes());

			log.info("JMS store size limit: {}.", jmsProperties.getStoreSizeLimit());
		}

		setupNetworkConnector(brokerService, channelProviders, discoveryAgent.orElse(new NoOpDiscoveryAgent()));

		try
		{
			brokerService.start();
		}
		catch (Exception e)
		{
			var storeIndex = new File(jmsProperties.getStorePath(), "db.data");

			if (storeIndex.exists())
			{
				log.error("Failed to start broker. Will try to rebuild JMS store index when app starts again.", e);

				if (!storeIndex.delete())
				{
					log.error("Failed to delete JMS store index file!");
				}
			}

			throw e;
		}

		return brokerService;
	}

	private PersistenceAdapter persistenceAdapter()
	{
		if (jmsProperties.getStoreType() == JmsProperties.StoreType.PERSISTENT)
		{
			KahaDBPersistenceAdapter adapter = new KahaDBPersistenceAdapter();
			adapter.setDirectory(new File(jmsProperties.getStorePath()));
			adapter.setCheckForCorruptJournalFiles(true);
			adapter.setChecksumJournalFiles(true);
			adapter.setIgnoreMissingJournalfiles(true);

			return adapter;
		}
		else
		{
			return new MemoryPersistenceAdapter();
		}
	}

	private void setupNetworkConnector(BrokerService brokerService, List<JmsBrokerChannelProvider> channelProviders, DiscoveryAgent discoveryAgent) throws Exception
	{
		var brokerQueues = new HashSet<>(jmsProperties.getBrokerChannels().getQueues());
		var brokerTopics = new HashSet<>(jmsProperties.getBrokerChannels().getTopics());

		channelProviders.forEach(channelProvider -> {
			brokerQueues.addAll(channelProvider.getQueues());
			brokerTopics.addAll(channelProvider.getTopics());
		});

		if (brokerQueues.isEmpty() && brokerTopics.isEmpty())
		{
			log.info("JMS broker has no external channels and won't be connected to broker network.");

			return;
		}

		var connector = new DiscoveryNetworkConnector();
		connector.setDuplex(jmsProperties.isDuplex());
		connector.setDiscoveryAgent(discoveryAgent);

		if (jmsProperties.getNetworkConnector() != null)
		{
			connector.setUri(new URI(jmsProperties.getNetworkConnector()));
		}

		brokerQueues.forEach(channel -> connector.addDynamicallyIncludedDestination(new ActiveMQQueue(channel)));
		brokerTopics.forEach(channel -> connector.addDynamicallyIncludedDestination(new ActiveMQTopic(channel)));

		brokerService.addNetworkConnector(connector);
	}

	private PolicyMap setupDestinationPolicies()
	{
		var policyEntry = new PolicyEntry();
		policyEntry.setQueue(">");
		policyEntry.setDeadLetterStrategy(buildDeadLetterStrategy(0));

		PolicyMap destinationPolicy = new PolicyMap();
		destinationPolicy.setDefaultEntry(policyEntry);

		jmsProperties.getChannels().forEach((channelName, channelPolicy) -> {
			var entry = new PolicyEntry();

			var channelDeadLetterPolicy = channelPolicy.getDeadLetterPolicy();

			if (channelDeadLetterPolicy != null)
			{
				if (!channelDeadLetterPolicy.isEnabled())
				{
					entry.setDeadLetterStrategy(new DiscardingDeadLetterStrategy());
				}
				else
				{
					entry.setDeadLetterStrategy(buildDeadLetterStrategy(channelDeadLetterPolicy.getExpiration()));
				}
			}
			else
			{
				entry.setDeadLetterStrategy(buildDeadLetterStrategy(0));
			}

			Optional.ofNullable(channelPolicy.getMemoryLimit()).ifPresent(value -> entry.setMemoryLimit(DataSize.parse(value, DataUnit.BYTES).toBytes()));
			Optional.ofNullable(channelPolicy.getCursorMemoryHighWaterMark()).ifPresent(entry::setCursorMemoryHighWaterMark);
			Optional.ofNullable(channelPolicy.getStoreUsageHighWaterMark()).ifPresent(entry::setStoreUsageHighWaterMark);

			if (channelPolicy.getChannelType() == JmsProperties.ChannelType.TOPIC)
			{
				entry.setTopic(channelName);

				Optional.ofNullable(channelPolicy.getPrefetch()).ifPresent(entry::setTopicPrefetch);

				destinationPolicy.put(new ActiveMQTopic(channelName), entry);
			}
			else
			{
				entry.setQueue(channelName);

				Optional.ofNullable(channelPolicy.getPrefetch()).ifPresent(entry::setQueuePrefetch);

				destinationPolicy.put(new ActiveMQQueue(channelName), entry);
			}
		});

		return destinationPolicy;
	}

	private DeadLetterStrategy buildDeadLetterStrategy(long expiration)
	{
		var strategy = new IndividualDeadLetterStrategy();
		strategy.setQueuePrefix(DLQ_PREFIX);
		strategy.setTopicPrefix(DLQ_PREFIX);
		strategy.setExpiration(expiration);

		return strategy;
	}
}
