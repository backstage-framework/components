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

package com.backstage.app.report.model;

import com.backstage.app.model.other.exception.AppStatusCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ReportAppStatusCode implements AppStatusCode
{
	REPORT_GENERATE_ERROR(1, "При генерации отчета произошла ошибка.");

	private final Integer range = AppStatusCode.MODULE_RANGE_REPORT;

	private final Integer code;

	private final String message;

	private final HttpStatusCode httpStatusCode;

	ReportAppStatusCode(Integer code, String message)
	{
		this(code, message, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	ReportAppStatusCode(Integer code, String message, HttpStatusCode httpStatusCode)
	{
		this.code = code;
		this.message = message.isEmpty() ? this.toString() : message;
		this.httpStatusCode = httpStatusCode;
	}
}
