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

package com.backstage.feign.interceptor;

import com.backstage.app.utils.SecurityUtils;
import com.backstage.feign.provider.PrincipalProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static com.backstage.feign.properties.FeignSessionProperties.GLOBAL_INTERCEPTOR_ACTIVATION_PROPERTY;

@Component
@ConditionalOnProperty(value = GLOBAL_INTERCEPTOR_ACTIVATION_PROPERTY, matchIfMissing = true)
@RequiredArgsConstructor
public class FeignSessionRequestFilter extends OncePerRequestFilter
{
	private final PrincipalProvider principalProvider;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException
	{
		var userId = request.getHeader(RequestHeaders.USER_ID.getName());

		if (userId == null)
		{
			filterChain.doFilter(request, response);

			return;
		}

		var principal = principalProvider.getPrincipal(userId);

		SecurityUtils.runForUser(principal, () -> filterChain.doFilter(request, response));
	}
}
