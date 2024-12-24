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

package com.backstage.app.dict.service.backend.mongo;

import com.backstage.app.dict.configuration.conditional.ConditionalOnEngine;
import com.backstage.app.dict.constant.ServiceFieldConstants;
import com.backstage.app.dict.domain.Dict;
import com.backstage.app.dict.domain.DictEngine;
import com.backstage.app.dict.domain.VersionScheme;
import com.backstage.app.dict.repository.mongo.MongoDictRepository;
import com.backstage.app.dict.service.backend.Engine;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnEngine(MongoEngine.MONGO)
public class MongoEngine implements Engine
{
	public static final String MONGO = "mongo";

	private final MongoTemplate mongoTemplate;
	private final MongoDictRepository mongoDictRepository;

	@Override
	public DictEngine getDictEngine()
	{
		return new DictEngine(MONGO);
	}

	@Override
	public void createDict()
	{
		mongoTemplate.createCollection(Dict.class);

		mongoDictRepository.findAll()
				.stream()
				.filter(dict -> dict.getVersion() == 0L)
				.peek(this::convertMongoServiceFields)
				.peek(dict -> dict.setVersion(1L))
				.forEach(mongoTemplate::save);
	}

	@Override
	public void createVersionScheme()
	{
		mongoTemplate.createCollection(VersionScheme.class);
	}

	@Override
	public boolean dictExists()
	{
		return mongoTemplate.collectionExists(Dict.class);
	}

	@Override
	public boolean versionSchemeExists()
	{
		return mongoTemplate.collectionExists(VersionScheme.class);
	}

	@Override
	public void dropDict()
	{
		mongoTemplate.dropCollection(Dict.class);
	}

	@Override
	public void dropVersionScheme()
	{
		mongoTemplate.dropCollection(VersionScheme.class);
	}

	private void convertMongoServiceFields(Dict dict)
	{
		dict.getFields()
				.stream()
				.filter(it -> it.getId().equals(ServiceFieldConstants._ID))
				.forEach(it -> it.setId(ServiceFieldConstants.ID));
	}
}
