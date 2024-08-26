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

package com.backstage.bpm.domain;

import com.backstage.bpm.conversion.jpa.ProcessParametersConverter;
import com.backstage.bpm.model.EngineType;
import com.backstage.app.database.model.UuidGeneratedEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.persistence.annotations.Convert;
import org.eclipse.persistence.annotations.Converter;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "process", schema = "#{@bpmProperties.ddl.scheme}")
@Converter(converterClass = ProcessParametersConverter.class, name = ProcessParametersConverter.NAME)
public class Process extends UuidGeneratedEntity
{
	@Enumerated(EnumType.STRING)
	@Column(name = "engine_type")
	private EngineType engineType;

	@Column(name = "session_id", nullable = false)
	private Long sessionId;

	@Column(name = "instance_id", nullable = false)
	private String instanceId;

	@Column(name = "workflow_id", nullable = false)
	private String workflowId;

	@Column(nullable = false)
	private LocalDateTime created;

	@Column(nullable = false)
	private boolean active;

	@Basic
	@Convert(ProcessParametersConverter.NAME)
	private Map<String, Object> parameters = new HashMap<>();

	@Version
	private Timestamp version;
}
