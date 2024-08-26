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

import com.backstage.bpm.conversion.jpa.TaskActionListConverter;
import com.backstage.bpm.conversion.jpa.TaskParametersConverter;
import com.backstage.bpm.conversion.jpa.TaskResultConverter;
import com.backstage.app.database.model.UuidGeneratedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.persistence.annotations.Convert;
import org.eclipse.persistence.annotations.Converter;
import org.eclipse.persistence.annotations.Index;
import org.eclipse.persistence.annotations.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Struct(name = "task")
@Entity
@Table(name = "task", schema = "#{@bpmProperties.ddl.scheme}")
@Converters({
		@Converter(converterClass = TaskResultConverter.class, name = TaskResultConverter.NAME),
		@Converter(converterClass = TaskActionListConverter.class, name = TaskActionListConverter.NAME),
		@Converter(converterClass = TaskParametersConverter.class, name = TaskParametersConverter.NAME)
})
public class Task extends UuidGeneratedEntity
{
	private String name;

	@Lob
	private String comment;

	@Column(name = "type", nullable = false)
	private String type;

	@Index(name = "ix_task_user_id")
	@Column(name = "user_id")
	private String userId;

	@Basic
	@Column(name = "candidate_user_ids")
	@Array(databaseType = "text")
	private List<String> candidateUserIds = new ArrayList<>();

	@Basic
	@Column(name = "user_roles")
	@Array(databaseType = "text")
	private List<String> userRoles = new ArrayList<>();

	@Column(nullable = false)
	private LocalDateTime created;

	@Version
	private Timestamp updated;

	private LocalDateTime completed;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TaskStatus status;

	@Basic
	@Convert(TaskParametersConverter.NAME)
	private Map<String, Object> parameters = new HashMap<>();

	@Basic
	@Convert(TaskActionListConverter.NAME)
	private List<TaskAction> actions = new ArrayList<>();

	@Convert(TaskResultConverter.NAME)
	private TaskResult result;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "fk_process", nullable = false)
	private Process process;

	@Setter(AccessLevel.NONE)
	@Column(name = "fk_process", insertable = false, updatable = false)
	private String processId;

	@Index(name = "ix_task_work_item_id")
	@Column(name = "work_item_id", nullable = false)
	private String workItemId;

	private LocalDateTime deadline;

	public void setProcess(Process process)
	{
		this.process = process;
		this.processId = process != null ? process.getId() : null;
	}
}
