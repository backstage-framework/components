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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "uuid_table")
public class UuidTable extends UuidGeneratedEntity
{
	private Double random;

	@OneToMany
	@JoinColumn(name = "fk_uuid_table")
	private List<VarcharTable> varcharTables = new ArrayList<>();

	@ManyToMany
	@JoinTable(name = "uuid_table_varchar_table",
			joinColumns = @JoinColumn(name = "fk_uuid_table"),
			inverseJoinColumns = @JoinColumn(name = "fk_varchar_table"))
	private Set<VarcharTable> varcharTableMany = new HashSet<>();
}
