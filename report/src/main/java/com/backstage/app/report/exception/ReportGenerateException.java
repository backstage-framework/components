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

package com.backstage.app.report.exception;

import com.backstage.app.exception.AppException;
import com.backstage.app.model.other.exception.ApiStatusCode;
import com.backstage.app.model.other.exception.ApiStatusCodeImpl;

public class ReportGenerateException extends AppException
{
	public ReportGenerateException(Throwable cause)
	{
		this(ApiStatusCodeImpl.REPORT_GENERATE_ERROR, cause);
	}

	public ReportGenerateException(ApiStatusCode status, Throwable cause)
	{
		super(status, cause);
	}

	public ReportGenerateException(String message, Throwable throwable)
	{
		super(ApiStatusCodeImpl.REPORT_GENERATE_ERROR, message, throwable);
	}
}
