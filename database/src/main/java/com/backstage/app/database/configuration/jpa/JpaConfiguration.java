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

package com.backstage.app.database.configuration.jpa;

import com.backstage.app.configuration.properties.AppProperties;
import com.backstage.app.database.configuration.conditional.ConditionalOnJpa;
import com.backstage.app.database.configuration.ddl.DDLConfiguration;
import com.backstage.app.database.configuration.jpa.customizer.EntityManagerFactoryCustomizer;
import com.backstage.app.database.configuration.jpa.eclipselink.Customizer;
import com.backstage.app.database.configuration.jpa.eclipselink.MetaModelVerifier;
import com.backstage.app.database.configuration.properties.JPAProperties;
import com.backstage.app.database.repository.CustomJpaRepositoryFactoryBean;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Configuration
@RequiredArgsConstructor
@EnableTransactionManagement
@ConditionalOnJpa
@EnableConfigurationProperties({JpaProperties.class, JPAProperties.class})
@PropertySource("classpath:jpa.properties")
@EnableJpaRepositories(basePackages = AppProperties.DEFAULT_PACKAGE, repositoryFactoryBeanClass = CustomJpaRepositoryFactoryBean.class)
public class JpaConfiguration
{
	// Убеждаемся, что при активированном DDL, он инициализируется до JPA.
	private final Optional<DDLConfiguration> ddlConfiguration;

	private final List<EntityManagerFactoryCustomizer> entityManagerFactoryCustomizers;

	@Bean
	public JpaVendorAdapter jpaVendorAdapter()
	{
		var eclipseLinkAdapter = new EclipseLinkJpaVendorAdapter();

		Optional.ofNullable(eclipseLinkAdapter.getJpaDialect())
				.ifPresent(dialect -> dialect.setLazyDatabaseTransaction(true));

		return eclipseLinkAdapter;
	}

	@Bean
	public Customizer jpaCustomizer()
	{
		return new Customizer();
	}

	@Bean
	public LocalContainerEntityManagerFactoryBean entityManagerFactory(AppProperties appProperties, JpaProperties jpaProperties, DataSource dataSource, Customizer customizer)
	{
		var packagesToScan = Stream.concat(appProperties.getBasePackages().stream(), entityManagerFactoryCustomizers.stream().flatMap(it -> it.getPackagesToScan().stream()))
				.distinct()
				.toArray(String[]::new);

		LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();

		emf.setDataSource(dataSource);
		emf.setJpaVendorAdapter(jpaVendorAdapter());
		emf.getJpaPropertyMap().putAll(jpaProperties.getProperties());
		emf.setPersistenceUnitName(appProperties.getModule());
		emf.setPersistenceUnitPostProcessors(entityManagerFactoryCustomizers.toArray(EntityManagerFactoryCustomizer[]::new));
		emf.setPackagesToScan(packagesToScan);
		emf.setMappingResources(entityManagerFactoryCustomizers.stream().flatMap(it -> it.getMappingResources().stream()).toArray(String[]::new));

		return emf;
	}

	@Bean
	public JpaTransactionManager jpaTransactionManager(EntityManagerFactory entityManagerFactory)
	{
		JpaTransactionManager jpaTransactionManager = new JpaTransactionManager();
		jpaTransactionManager.setEntityManagerFactory(entityManagerFactory);

		return jpaTransactionManager;
	}

	@Bean
	@ConditionalOnProperty(value = JPAProperties.META_MODEL_VALIDATION_ACTIVATION_PROPERTY, matchIfMissing = true)
	public MetaModelVerifier metaModelVerifier(AppProperties appProperties)
	{
		return new MetaModelVerifier(appProperties);
	}
}
