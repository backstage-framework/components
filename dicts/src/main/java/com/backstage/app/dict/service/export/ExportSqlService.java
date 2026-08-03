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

package com.backstage.app.dict.service.export;

import com.backstage.app.dict.api.domain.DictFieldType;
import com.backstage.app.dict.constant.ServiceFieldConstants;
import com.backstage.app.dict.domain.DictField;
import com.backstage.app.dict.domain.DictItem;
import com.backstage.app.dict.service.DictService;
import com.backstage.app.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DataType;
import org.jooq.SQLDialect;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultDSLContext;
import org.jooq.impl.SQLDataType;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportSqlService implements ExportService
{
	private final DictService dictService;

	public byte[] export(String dictId, List<DictItem> items, String userId)
	{
		var dict = dictService.getById(dictId);
		var dictFields = dict.getFields().stream()
				.filter(field -> !ServiceFieldConstants.HISTORY.equals(field.getId()))
				.toList();

		var context = new DefaultDSLContext(SQLDialect.POSTGRES);
		var columns = dictFields.stream().map(field -> DSL.field(field.getId(), mapFieldType(field))).toList();

		List<String> result = new ArrayList<>();

		result.add(context.createTable(dictId).columns(columns).getSQL());

		items.stream()
				.map(item -> context
						.insertInto(DSL.table(dictId), columns)
						.values(dictFields.stream().map(field -> mapValue(field, getFieldValue(field, item))).toList())
						.getSQL(ParamType.INLINED))
				.forEach(result::add);

		return String.join(";\n", result).getBytes(StandardCharsets.UTF_8);
	}

	private Object getFieldValue(DictField field, DictItem item)
	{
		return switch (field.getId())
		{
			case ServiceFieldConstants.ID -> item.getId();
			case ServiceFieldConstants.CREATED -> item.getCreated();
			case ServiceFieldConstants.UPDATED -> item.getUpdated();
			case ServiceFieldConstants.HISTORY -> item.getHistory();
			case ServiceFieldConstants.VERSION -> item.getVersion();

			default -> item.getData().get(field.getId());
		};
	}

	private DataType<?> mapFieldType(DictField field)
	{
		DataType<?> type = switch (field.getType())
		{
			case BOOLEAN -> SQLDataType.BOOLEAN;
			case SERIAL, INTEGER -> SQLDataType.BIGINT;
			case DATE -> SQLDataType.DATE;
			case TIMESTAMP -> SQLDataType.TIMESTAMPWITHTIMEZONE;
			case DECIMAL -> SQLDataType.DECIMAL;
			case GEO_JSON, JSON -> SQLDataType.JSONB;

			default -> SQLDataType.VARCHAR;
		};

		if (field.isMultivalued())
		{
			type = type.getArrayDataType();
		}

		return type;
	}

	private Object mapValue(DictField dictField, Object value)
	{
		if (dictField.getType() == DictFieldType.JSON || dictField.getType() == DictFieldType.GEO_JSON)
		{
			return JsonUtils.toJson(value);
		}

		return value;
	}
}
