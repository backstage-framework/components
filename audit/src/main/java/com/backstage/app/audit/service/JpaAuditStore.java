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

package com.backstage.app.audit.service;

import com.backstage.app.audit.model.domain.Audit;
import com.backstage.app.audit.model.domain.AuditProperties;
import com.backstage.app.audit.model.dto.AuditEvent;
import com.backstage.app.audit.model.other.AuditFilter;
import com.backstage.app.audit.repository.AuditRepository;
import com.backstage.app.utils.DateUtils;
import com.backstage.app.database.configuration.properties.DDLProperties;
import com.backstage.app.database.utils.StreamUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@RequiredArgsConstructor
public class JpaAuditStore implements AuditStore
{
	private final NamedParameterJdbcTemplate jdbc;

	private final AuditRepository auditRepository;

	private final com.backstage.app.audit.configuration.properties.AuditProperties auditConfigurationProperties;

	@Transactional
	public void write(AuditEvent event)
	{
		var audit = new Audit();

		audit.setType(event.getType());
		audit.setObjectId(event.getObjectId());
		audit.setUserId(event.getUserId());
		audit.setDate(DateUtils.toLocalDateTime(event.getDate()));
		audit.setSuccess(event.isSuccess());
		audit.setProperties(new AuditProperties());

		audit.getProperties().getFields().addAll(event.getFields());
		audit.getProperties().getProperties().addAll(event.getProperties());

		auditRepository.save(audit);
	}

	public Page<Audit> getByFilter(AuditFilter filter, Pageable pageable)
	{
		var parameters = new MapSqlParameterSource();
		var whereClauses = new HashSet<String>();

		if (pageable != null && pageable.isPaged())
		{
			parameters.addValue("limit", pageable.getPageSize())
					.addValue("offset", pageable.getOffset());
		}

		if (!filter.getTypes().isEmpty())
		{
			parameters.addValue("types", filter.getTypes());
			whereClauses.add("type in (:types)");
		}

		if (isNotBlank(filter.getObjectId()))
		{
			parameters.addValue("objectId", filter.getObjectId());
			whereClauses.add("object_id = :objectId");
		}

		if (isNotBlank(filter.getUserId()))
		{
			parameters.addValue("userId", filter.getUserId());
			whereClauses.add("user_id = :userId");
		}

		var fullTableName = Optional.of(auditConfigurationProperties.getDdl())
				.map(DDLProperties::getScheme)
				.map("%s.audit"::formatted)
				.orElse("audit");

		var sql = "from %s %s"
				.formatted(
						fullTableName,
						whereClauses.isEmpty() ? "" : "where " + String.join(" and ", whereClauses));

		var idsSql = "select id " + sql + " order by date desc" + (pageable != null && pageable.isPaged() ? " limit :limit offset :offset" : "");
		var countSql = "select count(id) " + sql;

		var ids = jdbc.queryForList(idsSql, parameters, String.class);
		var count = jdbc.queryForObject(countSql, parameters, Long.class);

		if (ids.isEmpty())
		{
			return new PageImpl<>(List.of(), pageable, count);
		}

		var items = auditRepository.findAll(ids).stream()
				.sorted(StreamUtils.listOrderComparator(ids))
				.collect(Collectors.toList());

		return new PageImpl<>(items, pageable, count);
	}

	public Page<Audit> getBySpecification(Specification<Audit> specification, Pageable pageable)
	{
		return auditRepository.findAll(specification, pageable);
	}

}
