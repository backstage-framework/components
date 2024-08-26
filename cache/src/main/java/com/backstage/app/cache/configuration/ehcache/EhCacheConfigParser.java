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

package com.backstage.app.cache.configuration.ehcache;

import com.backstage.app.cache.configuration.CacheSettings;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EhCacheConfigParser
{
	@Getter
	public static class EhCacheSettings extends CacheSettings
	{
		@XmlAttribute
		private String name;

		@XmlAttribute
		private int maxEntriesLocalHeap;

		@XmlAttribute
		private int timeToLiveSeconds;

		@XmlAttribute
		private int timeToIdleSeconds;
	}

	@Getter
	@XmlRootElement(name = "ehcache")
	public static class CacheConfig
	{
		@XmlElement(name = "cache")
		private final List<EhCacheSettings> caches = new ArrayList<>();
	}

	public static List<EhCacheSettings> parse()
	{
		try
		{
			var configStream = EhCacheConfigParser.class.getResourceAsStream("/ehcache.xml");

			if (configStream == null)
			{
				return List.of();
			}

			var jaxbContext = JAXBContext.newInstance(CacheConfig.class);
			var unmarshaller = jaxbContext.createUnmarshaller();

			return Optional.ofNullable((CacheConfig) unmarshaller.unmarshal(configStream))
					.map(CacheConfig::getCaches)
					.orElse(List.of());
		}
		catch (Exception e)
		{
			log.error("При загрузке конфигурации ehcache произошла ошибка.", e);

			return List.of();
		}
	}
}
