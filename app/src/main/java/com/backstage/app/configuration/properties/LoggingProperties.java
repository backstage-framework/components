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

package com.backstage.app.configuration.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties("app.logging")
public class LoggingProperties
{
	@Getter
	@Setter
	public static class LogbackProperties
	{
		/**
		 * Если установлено, то исключается конфиг по умолчанию и применяется customLogback.xml с classpath.
		 */
		boolean customConfig = false;
	}

	/**
	 * Если флаг установлен, то все логи пишутся в формате json.
	 */
	private boolean jsonOutput;

	private LogbackProperties logback = new LogbackProperties();
}
