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
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DictIndex
{
	private String id;

	private Sort.Direction direction;

	@ForceProxy
	@Builder.Default
	private List<String> fields = new ArrayList<>();

	public DictIndex copy()
	{
		return DictIndex.builder()
				.id(id)
				.direction(direction)
				.fields(new ArrayList<>(fields))
				.build();
	}
}
