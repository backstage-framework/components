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

package com.backstage.app.configuration.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

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

	@Getter
	@Setter
	public static class ConsoleProperties
	{
		/**
		 * Флаг позволяет отключить логирование в консоль.
		 */
		private boolean enabled = true;
	}

	@Getter
	@Setter
	public static class FileProperties
	{
		/**
		 * Флаг позволяет отключить логирование в файл.
		 */
		private boolean enabled = true;

		/**
		 * Максимальное количество архивов с лог-файлами в режиме ротации.
		 */
		int maxHistory = 100;

		/**
		 * Максимальный размер лог-файла, после которого он ротируется.
		 */
		DataSize maxSize = DataSize.ofMegabytes(30);

		/**
		 * Максимальный суммарный размер лог-файлов, при превышении которого старые архивы начинают удаляться.
		 */
		DataSize totalSizeCap = DataSize.ofBytes(0);
	}

	/**
	 * Если флаг установлен, то все логи пишутся в формате json.
	 */
	private boolean jsonOutput;

	private LogbackProperties logback = new LogbackProperties();

	private ConsoleProperties console = new ConsoleProperties();

	private FileProperties file = new FileProperties();
}
