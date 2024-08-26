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

import com.backstage.app.database.model.UuidGeneratedEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(schema = "#{@postgresTestProperties.ddl.scheme}")
@SecondaryTable(name = "ticket_information", pkJoinColumns = @PrimaryKeyJoinColumn(name = "fk_ticket"), schema = "#{@postgresTestProperties.ddl.scheme}")
public class Ticket extends UuidGeneratedEntity
{
	@Column(nullable = false)
	private LocalDateTime created;

	private String title;

	@Column(table = "ticket_information")
	private String description;

	@Column(name = "sender_email", nullable = false, table = "ticket_information")
	private String senderEmail;
}
