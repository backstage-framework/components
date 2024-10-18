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

package com.backstage.app.dict.configuration;

import com.backstage.app.cache.configuration.CacheSettingsProvider;
import com.backstage.app.cache.configuration.distributed.DistributedCacheOperations;
import com.backstage.app.cache.configuration.distributed.DistributedCacheSettings;
import com.backstage.app.configuration.conditional.ConditionalOnMissingQualifiedBean;
import com.backstage.app.dict.configuration.annotation.DictsMongoTemplate;
import com.backstage.app.dict.configuration.annotation.DictsPostgresDataSource;
import com.backstage.app.dict.configuration.annotation.DictsPostgresJdbcTemplate;
import com.backstage.app.dict.configuration.conditional.ConditionalOnEngine;
import com.backstage.app.dict.configuration.properties.DictsProperties;
import com.backstage.app.dict.exception.dict.DictException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Configuration
@EnableConfigurationProperties(DictsProperties.class)
public class DictsConfiguration
{
	public static final String CACHE_NAME_DICTS = "dicts";

	@Bean
	public CacheSettingsProvider dictsCacheSettingsProvider()
	{
		return new CacheSettingsProvider(List.of(DistributedCacheSettings.builder()
				.name(CACHE_NAME_DICTS)
				.maxEntriesLocalHeap(10000)
				.distributedOperations(Set.of(DistributedCacheOperations.DistributedCacheOperation.EVICT))
				.build()));
	}

	@Bean
	@DictsPostgresDataSource
	@ConditionalOnMissingQualifiedBean
	@ConditionalOnEngine("postgres")
	public DataSource dictsPostgresDataSource(Optional<DataSource> dataSource)
	{
		if (dataSource.isEmpty())
		{
			throw new DictException("no data source available");
		}

		return dataSource.get();
	}

	@Bean
	@DictsPostgresJdbcTemplate
	@ConditionalOnEngine("postgres")
	public NamedParameterJdbcTemplate dictsPostgresJdbcTemplate(@DictsPostgresDataSource DataSource dataSource)
	{
		return new NamedParameterJdbcTemplate(dataSource);
	}

	@Bean
	@DictsMongoTemplate
	@ConditionalOnMissingQualifiedBean
	@ConditionalOnEngine("mongo")
	@ConditionalOnClass(name = "org.springframework.data.mongodb.core.MongoTemplate")
	public MongoTemplate dictsMongoTemplate(Optional<MongoTemplate> mongoTemplate)
	{
		if (mongoTemplate.isEmpty())
		{
			throw new DictException("no mongo template available");
		}

		return mongoTemplate.get();
	}
}
