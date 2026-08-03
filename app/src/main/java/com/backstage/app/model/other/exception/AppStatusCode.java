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

package com.backstage.app.model.other.exception;

import org.springframework.http.HttpStatusCode;

public interface AppStatusCode
{
	Integer MODULE_RANGE_CORE = 0;
	Integer MODULE_RANGE_ATTACHMENTS = 100;
	Integer MODULE_RANGE_DICTS = 200;
	Integer MODULE_RANGE_REPORT = 300;

	Integer MODULE_RANGE_USER_APP = 1000;

	default Integer getRange()
	{
		return MODULE_RANGE_USER_APP;
	}

	Integer getCode();

	default Integer getStatusCode()
	{
		return getRange() + getCode();
	}

	String getMessage();

	HttpStatusCode getHttpStatusCode();
}
