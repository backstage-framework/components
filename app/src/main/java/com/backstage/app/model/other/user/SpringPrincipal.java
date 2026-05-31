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

package com.backstage.app.model.other.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public class SpringPrincipal implements Principal
{
	private final UserDetails userDetails;

	@Override
	public String getId()
	{
		return userDetails.getUsername();
	}

	public List<String> getPermissions()
	{
		return userDetails.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.map(it -> it.replace("ROLE_", ""))
				.collect(Collectors.toList());
	}
}
