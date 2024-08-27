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

import com.backstage.app.database.configuration.jpa.eclipselink.uuid.UuidSupportExtension;
import com.backstage.app.database.domain.UuidTable;
import com.backstage.app.database.domain.VarcharTable;
import com.backstage.app.database.model.UuidGeneratedEntity;
import com.backstage.app.database.repository.UuidTableRepository;
import com.backstage.app.database.repository.VarcharTableRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты проверяют работоспособность конвертации uuid.
 * В энтити все так же остается java.lang.String
 *
 * @see UuidSupportExtension
 */
class UuidSupportExtensionTest extends AbstractTest
{
	@Autowired
	UuidTableRepository repository;

	@Autowired
	VarcharTableRepository varcharTableRepository;

	@Autowired
	TransactionTemplate transactionTemplate;

	@Test
	void manyToOneEntityAsIdSelectTest()
	{
		assertThrowsExactly(NoSuchElementException.class,
				() -> varcharTableRepository.findByUuidTableId(UUID.randomUUID().toString())
						.orElseThrow());
	}

	@Test
	void insertUuidColumnIdTest()
	{
		var saved = transactionTemplate.execute(status -> {

			var entity = new UuidTable();
			entity.setRandom(Double.MAX_VALUE);

			var list = List.of(new VarcharTable(), new VarcharTable());
			list = varcharTableRepository.saveAll(list);

			entity.setVarcharTables(list);
			entity.getVarcharTableMany().addAll(list);

			return repository.save(entity);
		});

		assertNotNull(saved);

		saved = repository.findByIdEx(saved.getId());

		assertNotNull(saved);
		assertFalse(saved.getVarcharTables().isEmpty());
		assertFalse(saved.getVarcharTableMany().isEmpty());
		assertFalse(saved.getVarcharTables().get(0).getUuidTableMany().isEmpty());
	}

	@Test
	void selectUuidColumnIdTest()
	{
		var saved = transactionTemplate.execute(status -> {
			var entity = new UuidTable();
			entity.setRandom(Double.MAX_VALUE);

			var list = List.of(new VarcharTable(), new VarcharTable());
			list = varcharTableRepository.saveAll(list);

			entity.setVarcharTables(list);

			return repository.save(entity);
		});

		assertNotNull(saved);

		var table = repository.findByIdEx(saved.getId());

		assertFalse(table.getVarcharTables().isEmpty());

		var first = table.getVarcharTables().get(0);

		assertNotNull(first.getUuidTableId());
	}

	@Test
	void selectUuidColumnIdsTest()
	{
		var saved = transactionTemplate.execute(status -> repository.saveAll(List.of(
				new UuidTable(),
				new UuidTable(),
				new UuidTable()
		)));

		assertNotNull(saved);

		var items = repository.findAllById(saved.stream().map(UuidGeneratedEntity::getId).toList());

		assertFalse(items.isEmpty());
	}

	@Test
	void selectForeignColumnIdsTest()
	{
		var saved = transactionTemplate.execute(status -> {
			var entity = new UuidTable();
			entity.setRandom(Double.MAX_VALUE);

			var list = List.of(new VarcharTable(), new VarcharTable());
			list = varcharTableRepository.saveAll(list);

			entity.setVarcharTables(list);

			return repository.save(entity);
		});

		assertNotNull(saved);

		var items = varcharTableRepository.findByUuidTableIdIn(List.of(saved.getId()));

		assertFalse(items.isEmpty());
	}

	@Test
	void updateUuidColumnIdTest()
	{
		var entity = new UuidTable();
		entity.setRandom(Double.MAX_VALUE);

		transactionTemplate.execute(status ->
				repository.saveAndFlush(entity));

		transactionTemplate.executeWithoutResult(status -> {
			var uuidTable = repository.findByIdEx(entity.getId());
			uuidTable.setRandom(Double.MIN_VALUE);
		});
	}

	@Test
	void deleteUuidColumnIdTest()
	{
		var entity = new UuidTable();
		entity.setRandom(Double.MAX_VALUE);

		transactionTemplate.executeWithoutResult(status ->
				repository.saveAndFlush(entity));

		transactionTemplate.executeWithoutResult(status ->
				repository.deleteById(entity.getId()));
	}

	@Test
	void insertVarcharColumnIdTest()
	{
		var entity = new VarcharTable();
		entity.setRandom(Double.MAX_VALUE);

		varcharTableRepository.saveAndFlush(entity);
	}

	@Test
	void updateVarcharColumnIdTest()
	{
		var entity = new VarcharTable();
		entity.setRandom(Double.MAX_VALUE);

		transactionTemplate.execute(status ->
				varcharTableRepository.saveAndFlush(entity));

		transactionTemplate.executeWithoutResult(status -> {
			var uuidTable = varcharTableRepository.findByIdEx(entity.getId());
			uuidTable.setRandom(Double.MIN_VALUE);
		});
	}

	@Test
	void deleteVarcharColumnIdTest()
	{
		var entity = new VarcharTable();
		entity.setRandom(Double.MAX_VALUE);

		transactionTemplate.executeWithoutResult(status ->
				varcharTableRepository.saveAndFlush(entity));

		transactionTemplate.executeWithoutResult(status ->
				varcharTableRepository.deleteById(entity.getId()));
	}
}
