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

package com.backstage.app.dict.service.advice;

import com.backstage.app.attachment.configuration.properties.AttachmentProperties;
import com.backstage.app.attachment.service.AttachmentService;
import com.backstage.app.dict.api.domain.DictFieldType;
import com.backstage.app.dict.domain.Dict;
import com.backstage.app.dict.domain.DictField;
import com.backstage.app.dict.domain.DictItem;
import com.backstage.app.dict.service.DictDataService;
import com.backstage.app.model.other.user.UserInfo;
import com.backstage.app.utils.SpringContextUtils;
import com.google.common.base.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Order
@RequiredArgsConstructor
@ConditionalOnProperty(name = AttachmentProperties.ACTIVATION_PROPERTY, havingValue = "true")
public class AttachmentDictServiceAdvice implements DictServiceAdvice
{
	private static final String DEFAULT_USER_ID = UserInfo.SYSTEM_USER_ID;

	private final Supplier<AttachmentService> attachmentServiceSupplier = SpringContextUtils.createBeanSupplier(AttachmentService.class);
	private final Supplier<DictDataService> dictDataServiceSupplier = SpringContextUtils.createBeanSupplier(DictDataService.class);

	@Override
	public void handleBeforeUpdate(Dict oldDict, Dict dict)
	{
		var dictItems = getDictItems(oldDict);

		var oldAttachmentIds = getAttachmentsByItemId(oldDict, dictItems);
		var actualAttachmentIds = getAttachmentsByItemId(dict, dictItems);

		if (!oldAttachmentIds.equals(actualAttachmentIds))
		{
			var attachmentService = attachmentServiceSupplier.get();

			oldAttachmentIds.forEach((itemId, attachmentIds) ->
					releaseAttachments(attachmentService, dict.getId(), itemId, attachmentIds));

			actualAttachmentIds.forEach((itemId, attachmentIds) ->
					bindAttachments(attachmentService, dict.getId(), itemId, attachmentIds));
		}
	}

	@Override
	public void handleDelete(Dict dict)
	{
		var attachmentsByItemId = getAttachmentsByItemId(dict);

		var attachmentService = attachmentServiceSupplier.get();

		attachmentsByItemId.forEach((itemId, attachmentIds) ->
				releaseAttachments(attachmentService, dict.getId(), itemId, attachmentIds));
	}

	private void bindAttachments(AttachmentService attachmentService, String dictId, String itemId, List<String> attachmentIds)
	{
		if (!attachmentIds.isEmpty())
		{
			var attachmentOwnerId = getAttachmentOwnerId(dictId, itemId);
			attachmentService.bindAttachments(attachmentIds, DEFAULT_USER_ID,
					AttachmentDictDataServiceAdvice.DICT_ITEM_ATTACHMENT_TYPE, attachmentOwnerId);
		}
	}

	private void releaseAttachments(AttachmentService attachmentService, String dictId, String itemId, List<String> attachmentIds)
	{
		if (!attachmentIds.isEmpty())
		{
			attachmentService.releaseAttachments(AttachmentDictDataServiceAdvice.DICT_ITEM_ATTACHMENT_TYPE,
					getAttachmentOwnerId(dictId, itemId));
		}
	}

	private Map<String, List<String>> getAttachmentsByItemId(Dict dict)
	{
		var dictItems = getDictItems(dict);
		return getAttachmentsByItemId(dict, dictItems);
	}

	private List<DictItem> getDictItems(Dict dict)
	{
		return dictDataServiceSupplier.get()
				.getByFilter(dict.getId(), null, null, Pageable.unpaged())
				.toList();
	}

	private Map<String, List<String>> getAttachmentsByItemId(Dict dict, Collection<DictItem> dictItems)
	{
		var attachmentFields = getAttachmentFields(dict);

		return dictItems.stream()
				.collect(Collectors.groupingBy(
						DictItem::getId,
						Collectors.flatMapping(
								item -> getAttachmentIds(attachmentFields, item).stream(),
								Collectors.toList()
						)));
	}

	private Set<DictField> getAttachmentFields(Dict dict)
	{
		return dict.getFields()
				.stream()
				.filter(it -> it.getType() == DictFieldType.ATTACHMENT)
				.collect(Collectors.toSet());
	}

	private Collection<String> getAttachmentIds(Set<DictField> attachmentFieldIds, DictItem item)
	{
		return attachmentFieldIds.stream()
				.map(it -> getAttachmentIdsFromField(item, it))
				.flatMap(Collection::stream)
				.collect(Collectors.toSet());
	}

	public static String getAttachmentOwnerId(String dictId, String itemId)
	{
		return dictId + "_" + itemId;
	}

	@SuppressWarnings("unchecked")
	private List<String> getAttachmentIdsFromField(DictItem item, DictField dictField)
	{
		var value = item.getData()
				.get(dictField.getId());

		if (value == null)
		{
			return List.of();
		}

		if (dictField.isMultivalued())
		{
			return ((List<String>) value);
		}

		return List.of((String) value);
	}
}
