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

package com.backstage.app.database.annotation;

import com.backstage.app.database.AbstractTest;
import com.backstage.app.database.domain.Customer;
import com.backstage.app.database.domain.Product;
import com.backstage.app.database.repository.CustomerRepository;
import com.backstage.app.database.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ReadOnlyColumnAnnotationTest extends AbstractTest
{
	@Autowired ProductRepository productRepository;
	@Autowired CustomerRepository customerRepository;

	@Autowired TransactionTemplate transactionTemplate;

	@Test
	void readOnlyWithCollectionTableAnnotationWithoutSetReadOnlyAttributeTest()
	{
		transactionTemplate.executeWithoutResult(action ->
				productRepository.saveAll(List.of(
						new Product("apple"),
						new Product("banana"),
						new Product("orange"),
						new Product("pineapple"))
				));

		var savedCustomer = transactionTemplate.execute(action -> {
			var products = productRepository.findAll();
			assertFalse(products.isEmpty());

			var customer = new Customer();
			customer.setProducts(new HashSet<>(products));

			return customerRepository.save(customer);
		});

		var customer = customerRepository.findByIdEx(savedCustomer.getId());

		assertFalse(customer.getProductIds().isEmpty());
	}

	@Test
	void readOnlyWithCollectionTableAnnotationWithReadOnlyAttributeSetTest()
	{
		var result = transactionTemplate.execute(action -> {
			var customer = new Customer();

			return customerRepository.save(customer);
		});

		transactionTemplate.execute(action -> {
			var customer = customerRepository.findByIdEx(result.getId());
			customer.setProductIds(Set.of("banana_id"));

			return customerRepository.save(customer);
		});

		var savedCustomer = customerRepository.findByIdEx(result.getId());

		assertEquals(result.getProductIds(), savedCustomer.getProductIds());
	}
}
