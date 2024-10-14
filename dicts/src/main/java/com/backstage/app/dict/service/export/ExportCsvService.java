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

package com.backstage.app.dict.service.export;

import com.backstage.app.dict.constant.ServiceFieldConstants;
import com.backstage.app.dict.domain.DictField;
import com.backstage.app.dict.domain.DictItem;
import com.backstage.app.dict.service.DictService;
import com.backstage.app.dict.utils.CSVUtils;
import com.backstage.app.exception.AppException;
import com.backstage.app.model.other.exception.ApiStatusCodeImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportCsvService implements ExportService
{
	private final DictService dictService;

	public byte[] export(String dictId, List<DictItem> items, String userId)
	{
		var systemFieldIds = List.of(
				ServiceFieldConstants.ID,
				ServiceFieldConstants.CREATED,
				ServiceFieldConstants.UPDATED,
				ServiceFieldConstants.DELETED,
				ServiceFieldConstants.VERSION
		);

		var dict = dictService.getById(dictId);
		var dataFields = DictService.getDataFieldsByDict(dict)
				.stream()
				.toList();

		var headers = Stream.concat(
				systemFieldIds.stream(),
				dataFields.stream().map(DictField::getId)
		).toList();

		var data = items.stream()
				.map(item -> mapDictItem(dataFields, item))
				.collect(Collectors.toList());

		return writeToByteArray(headers, data);
	}

	private String[] mapDictItem(List<DictField> dictFields, DictItem item)
	{
		var builder = Stream.builder()
				.add(item.getId())
				.add(item.getCreated())
				.add(item.getUpdated())
				.add(item.getDeleted() != null)
				.add(item.getVersion());

		dictFields.stream()
				.map(it -> {
					var value = item.getData().getOrDefault(it.getId(), "");

					if (it.isMultivalued() && value instanceof Iterable<?> collection)
					{
						return CSVUtils.buildMultiValuedCell(collection);
					}

					return value;
				})
				.forEach(builder::add);

		return builder.build()
				.map(String::valueOf)
				.toArray(String[]::new);
	}

	private byte[] writeToByteArray(List<String> headers, List<String[]> data)
	{
		try (var stream = new ByteArrayOutputStream();
		     var streamWriter = new OutputStreamWriter(stream, StandardCharsets.UTF_8);
		     var writer = new CSVPrinter(streamWriter, CSVFormat.DEFAULT))
		{
			writer.printRecord(headers);
			writer.printRecords(data);

			streamWriter.flush();

			return stream.toByteArray();
		}
		catch (IOException e)
		{
			throw new AppException(ApiStatusCodeImpl.DICTS_ERROR, "При экспорте справочника произошла ошибка.", e);
		}
	}
}
