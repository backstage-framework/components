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

package com.backstage.app.report;

import com.backstage.app.database.configuration.jpa.DataSourceConfiguration;
import com.backstage.app.database.configuration.jpa.JpaConfiguration;
import com.backstage.app.report.configuration.ReportConfiguration;
import org.junit.ClassRule;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.LocalDate;

@WebMvcTest
@ContextConfiguration(classes = TestApp.class, initializers = {AbstractTests.Initializer.class})
@Import({ReportConfiguration.class, DataSourceConfiguration.class, JpaConfiguration.class, JacksonAutoConfiguration.class, JmsAutoConfiguration.class})
public class AbstractTests
{
	protected static final LocalDate FROM = LocalDate.of(2000, 1, 1);
	protected static final LocalDate TO = LocalDate.of(2000, 12, 31);

	@ClassRule
	public static final PostgreSQLContainer<?> postgres;

	static
	{
		postgres = new PostgreSQLContainer<>("postgres");
		postgres.start();
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
		}
	}
}
