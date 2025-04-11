/*
 *    Copyright 2019-2025 the original author or authors.
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

package com.backstage.app.dict.domain;

import com.backstage.app.cache.utils.proxy.ForceProxy;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dict
{
	private String id;

	private String name;

	@ForceProxy
	@Builder.Default
	private List<DictField> fields = new ArrayList<>();

	@ForceProxy
	@Builder.Default
	private List<DictIndex> indexes = new ArrayList<>();

	@ForceProxy
	@Builder.Default
	private List<DictConstraint> constraints = new ArrayList<>();

	@ForceProxy
	@Builder.Default
	private List<DictEnum> enums = new ArrayList<>();

	private String viewPermission;

	private String editPermission;

	private Integer maxHistory;

	private DictEngine engine;

	private Long version;

	public void setMaxHistory(Integer maxHistory)
	{
		if (maxHistory != null && maxHistory < 0)
		{
			throw new IllegalArgumentException("maxHistory must be >= 0");
		}

		this.maxHistory = maxHistory;
	}

	public List<String> getFieldIds()
	{
		return fields.stream()
				.map(DictField::getId)
				.toList();
	}

	public Dict copy()
	{
		List<DictField> fields = this.fields
				.stream()
				.map(DictField::copy)
				.collect(Collectors.toList());

		List<DictIndex> indexes = this.indexes
				.stream()
				.map(DictIndex::copy)
				.collect(Collectors.toList());

		List<DictConstraint> constraints = this.constraints
				.stream()
				.map(DictConstraint::copy)
				.collect(Collectors.toList());

		List<DictEnum> enums = this.enums
				.stream()
				.map(DictEnum::copy)
				.collect(Collectors.toList());

		return Dict.builder()
				.id(id)
				.name(name)
				.fields(fields)
				.indexes(indexes)
				.constraints(constraints)
				.enums(enums)
				.viewPermission(viewPermission)
				.editPermission(editPermission)
				.version(version)
				.engine(engine == null ? null : new DictEngine(engine.getName()))
				.maxHistory(maxHistory)
				.build();
	}
}
