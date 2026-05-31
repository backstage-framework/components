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

package com.backstage.app.dict.configuration.ddl;

import com.backstage.app.database.configuration.ddl.DDLConfiguration;
import com.backstage.app.database.configuration.ddl.DDLProvider;
import com.backstage.app.dict.configuration.properties.DictsProperties;
import com.backstage.app.dict.domain.Dict;
import com.backstage.app.dict.domain.VersionScheme;
import com.backstage.app.dict.service.backend.DictSchemeBackend;
import lombok.RequiredArgsConstructor;
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
@Order(DictsBackendDDLProvider.DDL_PRECEDENCE + 1)
public class DictsBackendDDLProvider implements DDLProvider
{
	public static final int DDL_PRECEDENCE = DDLConfiguration.DDL_PRECEDENCE_SYSTEM;

	private final DictsProperties dictsProperties;

	private final List<DictSchemeBackend> backends;

	@Override
	public void update()
	{
		if (dictsProperties.getDdl().isEnabled())
		{
			backends.forEach(DictSchemeBackend::applyDdl);
		}
	}
}
