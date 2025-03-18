/*
 *    Copyright 2019-2025 the original author or authors.
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

import com.backstage.app.dict.api.constant.ExportedDictFormat;
import com.backstage.app.dict.domain.DictItem;
import com.backstage.app.dict.model.export.ExportedResource;
import com.backstage.app.dict.service.DictDataService;
import com.backstage.app.dict.service.DictPermissionService;
import com.backstage.app.dict.service.DictService;
import com.backstage.app.dict.utils.ExportUtils;
import com.backstage.app.exception.AppException;
import com.backstage.app.model.other.exception.ApiStatusCodeImpl;
import com.backstage.app.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.backstage.app.dict.constant.ServiceFieldConstants.*;

@Service
@RequiredArgsConstructor
public class DictExportService
{
	private final ExportCsvService exportCsvService;
	private final ExportSqlService exportSqlService;
	private final ExportJsonService exportJsonService;

	private final DictService dictService;
	private final DictDataService dictDataService;
	private final DictPermissionService dictPermissionService;

	public ExportedResource exportToResource(String dictId, ExportedDictFormat format, List<String> itemIds)
	{
		return exportToResource(dictId, format, itemIds, null);
	}

	public ExportedResource exportToResource(String dictId, ExportedDictFormat format, List<String> itemIds, String query)
	{
		return exportToResource(dictId, format, itemIds, query, SecurityUtils.getCurrentUserId(), Sort.unsorted());
	}

	public ExportedResource  exportToResource(String dictId, ExportedDictFormat format, List<String> itemIds, String query, Sort sort)
	{
		return exportToResource(dictId, format, itemIds, query, SecurityUtils.getCurrentUserId(), sort);
	}

	public ExportedResource exportToResource(String dictId, ExportedDictFormat format, List<String> itemIds, String query, String userId, Sort sort)
	{
		var dict = dictService.getById(dictId);

		dictPermissionService.checkViewPermission(dict, userId);

		var items = (itemIds != null && !itemIds.isEmpty())
				? dictDataService.getByIds(dictId, itemIds, userId)
				: dictDataService.getByFilter(dictId, List.of(), query, Pageable.unpaged(), userId).getContent();

		var sortedItems = sortItems(items, sort);

		byte[] exportedData = getExportService(format)
				.export(dictId, sortedItems, userId);

		return ExportedResource.builder()
				.resource(new InputStreamResource(new ByteArrayInputStream(exportedData)))
				.filename(ExportUtils.generateFilename(dictId, format, itemIds))
				.build();
	}

	private ExportService getExportService(ExportedDictFormat format)
	{
		return switch (format)
				{
					case SQL -> exportSqlService;
					case CSV -> exportCsvService;
					case JSON -> exportJsonService;

					default -> throw new AppException(ApiStatusCodeImpl.ILLEGAL_DATA_FORMAT);
				};
	}

	private static List<DictItem> sortItems(List<DictItem> items, Sort sort)
	{
		if (sort.isUnsorted())
		{
			return items;
		}

		var comparator = sort.get()
				.map(order -> Comparator.comparing(
						(DictItem t) -> getFieldValue(t, order.getProperty()),
						Comparator.nullsLast(getDirection(order))
				))
				.reduce(Comparator::thenComparing)
				.orElseThrow(() -> new IllegalArgumentException("Невозможно создать компаратор."));

		var sortedList = new ArrayList<>(items);

		sortedList.sort(comparator);

		return sortedList;
	}

	@SuppressWarnings("unchecked")
	private static <T> Comparable<T> getFieldValue(DictItem item, String fieldName)
	{
		var value = switch (fieldName)
		{
			case ID -> item.getId();
			case CREATED -> item.getCreated();
			case UPDATED -> item.getUpdated();
			case DELETED -> item.getDeleted();
			case DELETION_REASON -> item.getDeletionReason();

			default -> getDataValue(item.getData(), fieldName);
		};

		if (value == null)
		{
			throw new AppException(ApiStatusCodeImpl.ILLEGAL_INPUT, "Сортировка по полю %s невозможна.".formatted(fieldName));
		}

		return (Comparable<T>) value;
	}

	private static Object getDataValue(Map<String, Object> data, String fieldName)
	{
		var value = data.get(fieldName);

		if (value instanceof Collection<?> collection && collection.iterator().hasNext())
		{
			return collection.iterator()
					.next();
		}

		return value;
	}

	private static Comparator<Comparable<Object>> getDirection(Sort.Order order)
	{
		return order.getDirection()
				.isAscending()
				? Comparator.naturalOrder()
				: Comparator.reverseOrder();
	}
}
