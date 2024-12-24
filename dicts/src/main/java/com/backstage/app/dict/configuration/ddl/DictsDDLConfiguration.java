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

package com.backstage.app.dict.configuration.ddl;

import com.backstage.app.configuration.AppConfiguration;
import com.backstage.app.dict.service.backend.VersionSchemeBackend;
import com.backstage.app.dict.service.lock.DictLockInitializer;
import com.backstage.app.dict.service.migration.ClasspathMigrationService;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigureAfter(AppConfiguration.class)
public class DictsDDLConfiguration
{
	@Bean
	public DictsDDLProvider dictsDDLProvider(DictLockInitializer dictLockInitializer,
	                                         ClasspathMigrationService classpathMigrationService,
	                                         VersionSchemeBackend versionSchemeBackend)
	{
		return new DictsDDLProvider(dictLockInitializer, classpathMigrationService, versionSchemeBackend);
	}
}
