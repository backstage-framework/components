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

package com.backstage.app.dict.service.backend.postgres;

import com.backstage.app.dict.configuration.conditional.ConditionalOnEngine;
import com.backstage.app.dict.configuration.properties.DictsProperties;
import com.backstage.app.dict.domain.VersionScheme;
import com.backstage.app.dict.exception.migration.MigrationAppliedException;
import com.backstage.app.dict.model.versionscheme.VersionSchemeColumnName;
import com.backstage.app.dict.service.backend.Engine;
import com.backstage.app.dict.service.backend.VersionSchemeBackend;
import com.backstage.app.dict.service.backend.postgres.rowmapper.VersionSchemeRowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@ConditionalOnEngine(PostgresEngine.POSTGRES)
public class PostgresVersionSchemeBackend extends AbstractPostgresBackend implements VersionSchemeBackend
{
	private final DictsProperties dictsProperties;

	@Override
	public Engine getEngine()
	{
		return postgresEngine;
	}

	@Override
	public List<VersionScheme> findAll()
	{
		var sql = "select * from %s.version_scheme".formatted(dictsProperties.getDdl().getScheme());

		return jdbc.query(sql, new VersionSchemeRowMapper());
	}

	@Override
	public VersionScheme saveVersionScheme(VersionScheme versionScheme)
	{
		return transactionWithResult(() -> createdVersionScheme(versionScheme), versionScheme.getVersion(), MigrationAppliedException::new);
	}

	@Override
	public Optional<VersionScheme> findByScript(String script)
	{
		var params = new MapSqlParameterSource();

		params.addValue(VersionSchemeColumnName.SCRIPT.getName(), script);

		var sql = "select * from %s.version_scheme where script = :script".formatted(dictsProperties.getDdl().getScheme());

		return Optional.of(jdbc.query(sql, params, new VersionSchemeRowMapper()))
				.stream()
				.flatMap(Collection::stream)
				.findFirst();
	}

	private VersionScheme createdVersionScheme(VersionScheme versionScheme)
	{
		var params = new MapSqlParameterSource();

		params.addValue(VersionSchemeColumnName.ID.getName(), versionScheme.getId());
		params.addValue(VersionSchemeColumnName.VERSION.getName(), versionScheme.getVersion());
		params.addValue(VersionSchemeColumnName.SCRIPT.getName(), versionScheme.getScript());
		params.addValue(VersionSchemeColumnName.CHECKSUM.getName(), versionScheme.getChecksum());
		params.addValue(VersionSchemeColumnName.INSTALLED.getName(), versionScheme.getInstalled());

		var sql = "insert into %s.version_scheme values (:id, :version, :script, :checksum, :installed)"
				.formatted(dictsProperties.getDdl().getScheme());

		jdbc.update(sql, params);

		return versionScheme;
	}
}
