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

import com.backstage.app.database.configuration.jpa.DataSourceConfiguration;
import com.backstage.app.database.configuration.jpa.JpaConfiguration;
import org.junit.ClassRule;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.clickhouse.ClickHouseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@ContextConfiguration(classes = TestApp.class, initializers = {AbstractTest.Initializer.class})
@Import({DataSourceConfiguration.class, JpaConfiguration.class, JacksonAutoConfiguration.class})
public class AbstractTest
{
	public static final String POSTGRES_IMAGE_NAME = "postgres:14";

	@ClassRule
	public static final PostgreSQLContainer<?> postgres;

	@ClassRule
	public static final ClickHouseContainer clickhouse;

	static
	{
		postgres = new PostgreSQLContainer<>(POSTGRES_IMAGE_NAME);
		postgres.start();

		clickhouse = new ClickHouseContainer("clickhouse/clickhouse-server:24.3.8");
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
					"app.clickhouse.port=" + clickhouse.getFirstMappedPort()
			).applyTo(configurableApplicationContext.getEnvironment());
		}
	}

	private static DockerImageName buildCustomPgImage()
	{
		var customPostgresImage = new ImageFromDockerfile()
				.withDockerfileFromBuilder(builder ->
						builder
								.from(POSTGRES_IMAGE_NAME)
								.env("DEBIAN_FRONTEND", "noninteractive")
								.run("apt-get update && apt-get install -y postgis postgresql-14-postgis-3 postgresql-14-partman --force-yes")
								.cmd("/usr/local/bin/docker-entrypoint.sh", "postgres")
								.build());

		return DockerImageName.parse(customPostgresImage.get())
				.asCompatibleSubstituteFor(PostgreSQLContainer.IMAGE);
	}
}
