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

package com.backstage.app.database.domain;

import com.backstage.app.database.configuration.jpa.eclipselink.annotation.ReadOnlyColumn;
import com.backstage.app.database.model.Identity;
import com.backstage.app.database.model.UuidGeneratedEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@Entity
@Table(name = "customer", schema = "#{@postgresTestProperties.ddl.scheme}")
public class Customer extends UuidGeneratedEntity
{
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "\"order\"",
			schema = "#{@postgresTestProperties.ddl.scheme}",
			joinColumns = @JoinColumn(name = "fk_customer"),
			inverseJoinColumns = @JoinColumn(name = "fk_order"))
	private Set<Product> products = new HashSet<>();

	@ReadOnlyColumn
	@ElementCollection
	@CollectionTable(name = "\"order\"", schema = "${app.test.postgres.ddl.scheme}", joinColumns = @JoinColumn(name = "fk_customer"))
	@Column(name = "fk_order")
	private Set<String> productIds = new HashSet<>();

	public void setProducts(Set<Product> products)
	{
		this.products = products;

		productIds = (products != null && !products.isEmpty())
				? products.stream()
				.map(Identity::getId)
				.collect(Collectors.toUnmodifiableSet())
				: Set.of();
	}
}
