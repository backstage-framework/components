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

import com.backstage.app.dict.configuration.conditional.ConditionalOnEngine;
import com.backstage.app.dict.domain.Dict;
import com.backstage.app.dict.domain.DictEnum;
import com.backstage.app.dict.exception.dict.DictNotFoundException;
import com.backstage.app.dict.exception.dict.enums.EnumNotFoundException;
import com.backstage.app.dict.service.backend.DictBackend;
import com.backstage.app.dict.service.backend.Engine;
import com.backstage.app.exception.AppException;
import com.backstage.app.model.other.exception.ApiStatusCodeImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@ConditionalOnEngine(MongoEngine.MONGO)
public class MongoDictBackend extends AbstractMongoBackend implements DictBackend
{
	public static final String _ID = "_id";

	@Override
	public Engine getEngine()
	{
		return mongoEngine;
	}

	@Override
	public Dict getDictById(String id)
	{
		Objects.requireNonNull(id, "dictId не может быть null.");

		return mongoDictRepository.findById(id)
				.orElseThrow(() -> new DictNotFoundException(id));
	}

	@Override
	public List<Dict> getAllDicts()
	{
		return mongoDictRepository.findAll();
	}

	@Override
	public Dict saveDict(Dict dict)
	{
		validate(dict);

		return save(dict);
	}

	@Override
	public Dict updateDict(Dict dict)
	{
		validate(dict);

		return save(dict);
	}

	@Override
	public void deleteById(String id)
	{
		mongoDictRepository.deleteById(id);
	}

	@Override
	public boolean existsById(String id)
	{
		return mongoDictRepository.existsById(id);
	}

	@Override
	public DictEnum createEnum(Dict dict, DictEnum dictEnum)
	{
		addTransactionData(null, true);

		save(dict);

		return dictEnum;
	}

	@Override
	public DictEnum updateEnum(Dict dict, DictEnum dictEnum)
	{
		addTransactionData(null, true);

		var oldEnum = dict.getEnums()
				.stream()
				.filter(it -> it.getId().equals(dictEnum.getId()))
				.findAny()
				.orElseThrow(() -> new EnumNotFoundException(dictEnum.getId()));

		oldEnum.setValues(dictEnum.getValues());
		oldEnum.setName(dictEnum.getName());

		save(dict);

		return dictEnum;
	}

	@Override
	public void deleteEnum(Dict dict, String enumId)
	{
		addTransactionData(null, true);

		save(dict);
	}

	private Dict save(Dict dict)
	{
		return mongoDictRepository.save(dict);
	}

	private void validate(Dict dict)
	{
		dict.getFieldIds()
				.stream()
				.filter(_ID::equals)
				.findAny()
				.ifPresent(it -> {
					throw new AppException(
							ApiStatusCodeImpl.ILLEGAL_INPUT,
							"Недопустимо использование пользовательского поля 'id' для Mongo."
					);
				});
	}
}
