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

import com.backstage.app.dict.api.domain.DictFieldType;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DictField
{
	private String id;

	private String name;

	private DictFieldType type;

	private DictFieldName dictRef;

	private String enumId;

	private boolean multivalued;

	private boolean required;

	private Number minSize;

	private Number maxSize;

	private Object defaultValue;

	public DictField copy()
	{
		return DictField.builder()
				.id(this.id)
				.name(this.name)
				.type(this.type)
				.dictRef(this.dictRef)
				.enumId(this.enumId)
				.multivalued(this.multivalued)
				.required(this.required)
				.minSize(this.minSize)
				.maxSize(this.maxSize)
				.defaultValue(this.defaultValue)
				.build();
	}
}
