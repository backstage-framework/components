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

import com.backstage.app.dict.api.domain.DictFieldType;
import com.backstage.app.dict.configuration.conditional.ConditionalOnEngine;
import com.backstage.app.dict.constant.ServiceFieldConstants;
import com.backstage.app.dict.domain.Dict;
import com.backstage.app.dict.domain.DictConstraint;
import com.backstage.app.dict.domain.DictField;
import com.backstage.app.dict.domain.DictIndex;
import com.backstage.app.dict.domain.scheme.DictNativeScheme;
import com.backstage.app.dict.domain.scheme.FieldNativeScheme;
import com.backstage.app.dict.exception.dict.DictAlreadyExistsException;
import com.backstage.app.dict.exception.dict.DictNotFoundException;
import com.backstage.app.dict.exception.dict.enums.EnumNotFoundException;
import com.backstage.app.dict.service.backend.DictSchemeBackend;
import com.backstage.app.dict.service.backend.Engine;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoNamespace;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.schema.JsonSchemaProperty;
import org.springframework.data.mongodb.core.schema.MongoJsonSchema;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;

import static java.util.function.Predicate.not;
import static org.springframework.data.mongodb.core.schema.JsonSchemaProperty.*;

@Component
@RequiredArgsConstructor
@ConditionalOnEngine(MongoEngine.MONGO)
public class MongoDictSchemeBackend extends AbstractMongoBackend implements DictSchemeBackend
{
	private final MongoSequenceService mongoSequenceService;

	@Override
	public Engine getEngine()
	{
		return mongoEngine;
	}

	@Override
	public void applyDdl()
	{
	}

	@Override
	public void createDictScheme(Dict dict)
	{
		var id = dict.getId();

		if (existsDictSchemeById(id))
		{
			throw new DictAlreadyExistsException(id);
		}

		addTransactionData(dict, true);

		var mongoDict = dict.copy();
		addMongoServiceFields(mongoDict.getFields());

		mongoTemplate.createCollection(id, buildCollectionOptions(mongoDict));

//		TODO: Валидация индексов при создании
		mongoDict.getIndexes()
				.forEach(it -> mongoTemplate.indexOps(id).ensureIndex(buildIndex(it)));

	}

	@Override
	public void updateDictScheme(Dict updatedDict)
	{
		addTransactionData(updatedDict, true);

		var mongoDict = updatedDict.copy();
		addMongoServiceFields(mongoDict.getFields());

		var params = new LinkedHashMap<String, Object>();
		params.put("collMod", mongoDict.getId());
		params.put("validator", buildMongoJsonSchema(mongoDict).toDocument());

		mongoTemplate.executeCommand(new Document(params));

	}

	@Override
	public void renameDictSchemeById(String dictId, String renamedDictId)
	{
		if (!existsDictSchemeById(dictId))
		{
			throw new DictNotFoundException(dictId);
		}

		mongoTemplate.getCollection(dictId)
				.renameCollection(new MongoNamespace(mongoTemplate.getDb().getName(), renamedDictId));
	}

	@Override
	public void deleteDictSchemeById(String dictId)
	{
		if (!existsDictSchemeById(dictId))
		{
			throw new DictNotFoundException(dictId);
		}

		mongoTemplate.dropCollection(dictId);
	}

	@Override
	public boolean existsDictSchemeById(String dictId)
	{
		return mongoTemplate.collectionExists(dictId);
	}

	@Override
	public DictField renameDictField(Dict dict, String oldFieldId, DictField field)
	{
		addTransactionData(dict, true);

		if (field.getType() == DictFieldType.SERIAL)
		{
			restartSerialField(dict.getId(), field.getId(), mongoSequenceService.getSequenceValue(dict.getId(), oldFieldId) + 1);
			restartSerialField(dict.getId(), oldFieldId, 1L);
		}

		if (!oldFieldId.equals(field.getId()))
		{
			var updateQuery = new BasicDBObject();
			updateQuery.append("$rename", new BasicDBObject().append(oldFieldId, field.getId()));
			mongoTemplate.getCollection(dict.getId()).updateMany(new BasicDBObject(), updateQuery);
		}

		return field;
	}

	@Override
	public DictConstraint createConstraint(Dict dict, DictConstraint constraint)
	{
		addTransactionData(dict, true);

		mongoTemplate.indexOps(dict.getId()).ensureIndex(buildIndex(constraint));

		return constraint;
	}

	@Override
	public void deleteConstraint(Dict dict, String id)
	{
		addTransactionData(dict, true);

		mongoTemplate.indexOps(dict.getId()).dropIndex(id);
	}

	@Override
	public DictIndex createIndex(Dict dict, DictIndex index)
	{
		addTransactionData(dict, true);

		mongoTemplate.indexOps(dict.getId()).ensureIndex(buildIndex(index));

		return index;
	}

	@Override
	public void deleteIndex(Dict dict, String id)
	{
		addTransactionData(dict, true);

		mongoTemplate.indexOps(dict.getId()).dropIndex(id);
	}

	@Override
	public DictNativeScheme getNativeScheme(Dict dict)
	{
		var fieldsNativeScheme = dict.getFields()
				.stream()
				.map(it -> getFieldNativeScheme(dict.getId(), it))
				.toList();

		return DictNativeScheme.builder()
				.dictId(dict.getId())
				.engine(dict.getEngine())
				.tableId(dict.getId())
				.fields(fieldsNativeScheme)
				.build();
	}

	@Override
	public void restartSerialField(String dictId, String fieldId, Long startWithValue)
	{
		mongoSequenceService.setSequenceValue(dictId, fieldId, startWithValue);
	}

	private static FieldNativeScheme getFieldNativeScheme(String tableId, DictField field)
	{
		return FieldNativeScheme.builder()
				.fieldId(field.getId())
				.columnId(field.getId())
				.fullColumnId("%s.%s".formatted(tableId, field.getId()))
//						FIXME: Заглушка. Необходимо определить, что считаем целевым типом - bson-тип, внутренние типы mongoDB или что-то третье
				.nativeType(null)
				.build();
	}

	private CollectionOptions buildCollectionOptions(Dict dict)
	{
		return CollectionOptions.empty()
				.schema(buildMongoJsonSchema(dict));
	}

	private Index buildIndex(DictConstraint source)
	{
		var target = new Index().named(source.getId());

		source.getFields().forEach(it -> target.on(it, Sort.Direction.ASC));

		target.unique();

		return target;
	}

	//TODO: провести рефакторинг
	private MongoJsonSchema buildMongoJsonSchema(Dict dict)
	{
		var builder = MongoJsonSchema.builder();

		dict.getFields()
				.stream()
				.filter(DictField::isRequired)
				.filter(not(it -> it.getType() == DictFieldType.SERIAL || it.getType() == DictFieldType.ENUM || it.getType() == DictFieldType.JSON || it.getType() == DictFieldType.GEO_JSON))
				.forEach(it -> {
					var property = getPropertyByDictField(it.getType(), it.getId());

					builder.property(required(it.isMultivalued()
							? array(it.getId()).items(property)
							: property));
				});

		dict.getFields()
				.stream()
				.filter(DictField::isRequired)
				.filter(it -> it.getType() == DictFieldType.ENUM)
				.forEach(it -> {
					var property = dict.getEnums()
							.stream()
							.filter(e -> e.getId().equals(it.getEnumId()))
							.findFirst()
							.map(e -> string(it.getId()).possibleValues(e.getValues()))
							.orElseThrow(() -> new EnumNotFoundException(it.getEnumId()));

					builder.property(required(it.isMultivalued()
							? array(it.getId()).items(property)
							: property));
				});

		return builder.build();
	}

	private JsonSchemaProperty getPropertyByDictField(DictFieldType type, String fieldId)
	{
		return switch (type)
				{
					case SERIAL, INTEGER -> int64(fieldId);
					case DECIMAL -> decimal128(fieldId);
					case STRING, DICT, ATTACHMENT -> string(fieldId);
					case BOOLEAN -> named(fieldId).ofType(new Type.JsonType("boolean"));
					case DATE, TIMESTAMP -> date(fieldId);
					case GEO_JSON -> object(fieldId);

					default -> throw new RuntimeException("unsupported type: %s".formatted(type));
				};
	}

	//TODO: подумать над тем, как сделать это обязательным контрактом
	// чтобы при реализации дополнительного адаптера, характерные сервисные поля были добавлены, а не пропущены.
	private void addMongoServiceFields(List<DictField> dictFields)
	{
		dictFields.stream()
				.filter(it -> it.getId().equals(ServiceFieldConstants.ID))
				.forEach(it -> it.setId(MongoDictBackend._ID));
	}
}
