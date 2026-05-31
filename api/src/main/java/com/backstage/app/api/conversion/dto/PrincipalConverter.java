/*
 *    Copyright 2019-2026 the original author or authors.
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

package com.backstage.app.api.conversion.dto;

import com.backstage.app.api.model.dto.PrincipalDto;
import com.backstage.app.conversion.dto.AbstractConverter;
import com.backstage.app.model.other.user.Principal;
import com.backstage.app.service.user.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PrincipalConverter extends AbstractConverter<Principal, PrincipalDto>
{
	private final PermissionService permissionService;

	private final Optional<PrincipalDetailsConverter> detailsConverter;

	@Override
	public PrincipalDto convert(Principal source)
	{
		var details = detailsConverter
				.map(converter -> converter.convert(source))
				.orElse(null);

		return PrincipalDto.builder()
				.id(source.getId())
				.permissions(permissionService.getPermissions(source.getId()))
				.details(details)
				.build();
	}
}
