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

package com.backstage.app.dict.dto;

import com.backstage.app.conversion.dto.AbstractConverter;
import com.backstage.app.dict.api.dto.request.CreateDictFieldRequest;
import com.backstage.app.dict.domain.DictField;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DictFieldRequestConverter extends AbstractConverter<CreateDictFieldRequest, DictField>
{
	private final DictFieldNameRequestConverter fieldNameConverter;

	@Override
	public DictField convert(CreateDictFieldRequest source)
	{
		return DictField.builder()
				.id(source.getId())
				.dictRef(source.getDictRef() != null ? fieldNameConverter.convert(source.getDictRef()) : null)
				.enumId(source.getEnumId())
				.maxSize(source.getMaxSize())
				.name(source.getName())
				.minSize(source.getMinSize())
				.multivalued(source.isMultivalued())
				.required(source.isRequired())
				.type(source.getType())
				.defaultValue(source.getDefaultValue())
				.build();
	}
}
