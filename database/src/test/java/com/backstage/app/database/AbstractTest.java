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

import org.junit.ClassRule;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.testcontainers.clickhouse.ClickHouseContainer;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
@EnableWebMvc
@ContextConfiguration(classes = TestApp.class, initializers = {AbstractTest.Initializer.class})
@Import({JacksonAutoConfiguration.class})
public class AbstractTest
{
	public static final String POSTGRES_IMAGE_NAME = "postgres:16";

	public static final String CLICKHOUSE_SECRET = "clickhouse";

	@ClassRule
	public static final PostgreSQLContainer<?> postgres;

	@ClassRule
	public static final ClickHouseContainer clickhouse;

	static
	{
		postgres = new PostgreSQLContainer<>(POSTGRES_IMAGE_NAME);
		postgres.start();

		clickhouse = new ClickHouseContainer("clickhouse/clickhouse-server")
				.withUsername(CLICKHOUSE_SECRET)
				.withPassword(CLICKHOUSE_SECRET);
		clickhouse.start();
	}

	static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext>
	{
		@Override
		public void initialize(ConfigurableApplicationContext configurableApplicationContext)
		{
			TestPropertyValues.of(
					"app.dataSource.driverClassName=" + postgres.getDriverClassName(),
					"app.dataSource.url=" + postgres.getJdbcUrl(),
					"app.dataSource.username=" + postgres.getUsername(),
					"app.dataSource.password=" + postgres.getPassword()
			).applyTo(configurableApplicationContext.getEnvironment());

			TestPropertyValues.of(
					"app.clickhouse.host=" + clickhouse.getHost(),
					"app.clickhouse.port=" + clickhouse.getFirstMappedPort(),
					"app.clickhouse.username=" + CLICKHOUSE_SECRET,
					"app.clickhouse.password=" + CLICKHOUSE_SECRET
			).applyTo(configurableApplicationContext.getEnvironment());
		}
	}
}
