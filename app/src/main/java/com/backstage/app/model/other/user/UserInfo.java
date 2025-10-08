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

package com.backstage.app.model.other.user;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class UserInfo implements Principal
{
	public static UserInfo ANONYMOUS_USER;
	public static UserInfo SYSTEM_USER;

	public static final String ANONYMOUS_USER_ID = new UUID(0, 1).toString();
	public static final String ANONYMOUS_USER_NAME = "anonymous";

	public static final String SYSTEM_USER_ID = new UUID(0, 0).toString();
	public static final String SYSTEM_USER_NAME = "system";

	static
	{
		ANONYMOUS_USER = UserInfo.builder()
				.id(ANONYMOUS_USER_ID)
				.type(UserType.LOCAL)
				.email(ANONYMOUS_USER_ID + "@local")
				.firstName(ANONYMOUS_USER_NAME)
				.lastName(ANONYMOUS_USER_NAME)
				.build();

		SYSTEM_USER = UserInfo.builder()
				.id(SYSTEM_USER_ID)
				.type(UserType.LOCAL)
				.email(SYSTEM_USER_ID + "@local")
				.firstName(SYSTEM_USER_NAME)
				.lastName(SYSTEM_USER_NAME)
				.build();
	}

	private String id;

	private String firstName;

	private String lastName;

	private String email;

	private UserType type;
}
