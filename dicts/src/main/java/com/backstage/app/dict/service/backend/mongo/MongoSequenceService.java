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

package com.backstage.app.dict.service.backend.mongo;

import com.backstage.app.dict.configuration.annotation.DictsMongoTemplate;
import com.backstage.app.dict.configuration.conditional.ConditionalOnEngine;
import com.backstage.app.dict.model.mongo.MongoCounter;
import com.backstage.app.utils.TaskUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnEngine(MongoEngine.MONGO)
public class MongoSequenceService
{
	public static final int REQUEST_TRY_COUNT = 3;
	public static final int REQUEST_DELAY_MS = 100;

	@DictsMongoTemplate
	private final MongoTemplate mongoTemplate;

	public long getSequenceValue(String dictId, String fieldId)
	{
		var query = new Query(Criteria.where("_id").is(getSequenceName(dictId, fieldId)));

		var counter = TaskUtils.executeWithTryCount(REQUEST_TRY_COUNT, REQUEST_DELAY_MS,
				() -> mongoTemplate.findOne(query, MongoCounter.class, "counters"),
				(tryCount, throwable) -> throwable instanceof DataAccessException);

		return counter != null ? counter.getSeq() : 0;
	}

	public long getNextSequenceValue(String dictId, String fieldId)
	{
		var query = new Query(Criteria.where("_id").is(getSequenceName(dictId, fieldId)));
		var update = new Update().inc("seq", 1);

		FindAndModifyOptions options = new FindAndModifyOptions()
				.returnNew(true)
				.upsert(true);

		var counter = TaskUtils.executeWithTryCount(REQUEST_TRY_COUNT, REQUEST_DELAY_MS,
				() -> mongoTemplate.findAndModify(query, update, options, MongoCounter.class, "counters"),
				(tryCount, throwable) -> throwable instanceof DataAccessException);

		return counter.getSeq();
	}

	public void setSequenceValue(String dictId, String fieldId, long startWithValue)
	{
		var query = new Query(Criteria.where("_id").is(getSequenceName(dictId, fieldId)));
		var update = new Update().set("seq", startWithValue - 1);

		FindAndModifyOptions options = new FindAndModifyOptions()
				.returnNew(true)
				.upsert(true);

		TaskUtils.executeWithTryCount(REQUEST_TRY_COUNT, REQUEST_DELAY_MS,
				() -> mongoTemplate.findAndModify(query, update, options, MongoCounter.class, "counters"),
				(tryCount, throwable) -> throwable instanceof DataAccessException);
	}

	public void deleteSequence(String name)
	{
		var query = new Query(Criteria.where("_id").is(name));

		TaskUtils.executeWithTryCount(REQUEST_TRY_COUNT, REQUEST_DELAY_MS,
				() -> mongoTemplate.remove(query, "counters"),
				(tryCount, throwable) -> throwable instanceof DataAccessException);
	}

	private String getSequenceName(String dictId, String fieldId)
	{
		return dictId + "_" + fieldId;
	}
}
