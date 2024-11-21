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

import com.backstage.app.database.configuration.ddl.DDLConfiguration;
import com.backstage.app.database.configuration.ddl.DDLProvider;
import com.backstage.app.dict.configuration.properties.DictsProperties;
import com.backstage.app.dict.domain.Dict;
import com.backstage.app.dict.domain.VersionScheme;
import com.backstage.app.dict.exception.EngineException;
import com.backstage.app.dict.service.backend.Engine;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Провайдер, инициирующий создание {@link Dict}/{@link VersionScheme}
 * в целевом адаптере, указанном в {@link DictsProperties#getStorage()} при первом запуске приложения.
 * Провайдер должен быть выполнен раньше {@link DictsStorageMigrationDDLProvider}, по причине наличия обьектов
 * {@link Dict}/{@link VersionScheme} в источниках данных, в том числе при первой инициализации.
 */
@Component
@RequiredArgsConstructor
@Order(DictsStorageDDLProvider.DDL_PRECEDENCE)
public class DictsStorageDDLProvider implements DDLProvider
{
	public static final int DDL_PRECEDENCE = DDLConfiguration.DDL_PRECEDENCE_SYSTEM;

	private final DictsProperties dictsProperties;

	private final List<Engine> engines;

	@Override
	public void update()
	{
		var firstInit = engines.stream()
				.allMatch(it -> !it.dictExists() && !it.versionSchemeExists());

		if (firstInit)
		{
			var engine = engines.stream()
					.filter(it -> StringUtils.equalsAny(it.getDictEngine().getName(), dictsProperties.getStorage()))
					.toList()
					.stream()
					.findFirst()
					.orElseThrow(() -> new EngineException("An engine equal to the engine from the storage property doesn't exists."));

			engine.createDict();
			engine.createVersionScheme();
		}
	}
}
