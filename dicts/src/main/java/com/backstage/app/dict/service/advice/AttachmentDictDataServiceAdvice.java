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

package com.backstage.app.dict.service.advice;

import com.backstage.app.attachment.configuration.properties.AttachmentProperties;
import com.backstage.app.attachment.service.AttachmentService;
import com.backstage.app.attachment.utils.AttachmentBindingUtils;
import com.backstage.app.dict.api.domain.DictFieldType;
import com.backstage.app.dict.domain.Dict;
import com.backstage.app.dict.domain.DictField;
import com.backstage.app.dict.domain.DictItem;
import com.backstage.app.model.other.user.UserInfo;
import com.backstage.app.utils.SpringContextUtils;
import com.google.common.base.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Order
@RequiredArgsConstructor
@ConditionalOnProperty(name = AttachmentProperties.ACTIVATION_PROPERTY, havingValue = "true")
public class AttachmentDictDataServiceAdvice implements DictDataServiceAdvice
{
	public static final String DICT_ITEM_ATTACHMENT_TYPE = "DICT_ITEM";

	private static final String DEFAULT_USER_ID = UserInfo.SYSTEM_USER_ID;

	private final Supplier<AttachmentService> attachmentServiceSupplier = SpringContextUtils.createBeanSupplier(AttachmentService.class);

	@Override
	public void handleAfterCreate(Dict dict, DictItem item)
	{
		bindAttachments(attachmentServiceSupplier.get(), dict, item);
	}

	@Override
	public void handleUpdate(Dict dict, DictItem oldItem, DictItem item)
	{
		var attachmentService = attachmentServiceSupplier.get();

		var oldAttachmentIds = getAttachmentIds(dict, oldItem);
		var actualAttachmentIds = getAttachmentIds(dict, item);

		if (!oldAttachmentIds.equals(actualAttachmentIds))
		{
			releaseAttachments(attachmentService, dict, oldItem, oldAttachmentIds);
			bindAttachments(attachmentService, dict, item, actualAttachmentIds);
		}
	}

	@Override
	public void handleDelete(Dict dict, DictItem item)
	{
		releaseAttachments(attachmentServiceSupplier.get(), dict, item);
	}

	@Override
	public void handleDeleteAll(Dict dict)
	{
		var attachmentService = attachmentServiceSupplier.get();

		attachmentService.releaseAttachments(DICT_ITEM_ATTACHMENT_TYPE, AttachmentBindingUtils.buildObjectIdPattern(dict.getId()));
	}

	private void bindAttachments(AttachmentService attachmentService, Dict dict, DictItem item)
	{
		bindAttachments(attachmentService, dict, item, getAttachmentIds(dict, item));
	}

	private void bindAttachments(AttachmentService attachmentService, Dict dict, DictItem item, Set<String> attachmentIds)
	{
		if (!attachmentIds.isEmpty())
		{
			attachmentService.bindAttachments(attachmentIds, DEFAULT_USER_ID, DICT_ITEM_ATTACHMENT_TYPE, getAttachmentOwnerId(dict, item));
		}
	}

	private void releaseAttachments(AttachmentService attachmentService, Dict dict, DictItem item)
	{
		releaseAttachments(attachmentService, dict, item, getAttachmentIds(dict, item));
	}

	private void releaseAttachments(AttachmentService attachmentService, Dict dict, DictItem item, Set<String> attachmentIds)
	{
		if (!attachmentIds.isEmpty())
		{
			attachmentService.releaseAttachments(DICT_ITEM_ATTACHMENT_TYPE, getAttachmentOwnerId(dict, item));
		}
	}

	public static String getAttachmentOwnerId(Dict dict, DictItem item)
	{
		return getAttachmentOwnerId(dict.getId(), item);
	}

	public static String getAttachmentOwnerId(String dictId, DictItem item)
	{
		return AttachmentBindingUtils.buildComplexObjectId(dictId, item.getId());
	}

	private Set<String> getAttachmentIds(Dict dict, DictItem item)
	{
		return getAttachmentDictFieldIds(dict)
				.stream()
				.map(field -> getAttachmentIdsFromField(item, field))
				.flatMap(Collection::stream)
				.collect(Collectors.toSet());
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

	private List<DictField> getAttachmentDictFieldIds(Dict dict)
	{
		return dict.getFields()
				.stream()
				.filter(it -> it.getType().equals(DictFieldType.ATTACHMENT))
				.toList();
	}
}
