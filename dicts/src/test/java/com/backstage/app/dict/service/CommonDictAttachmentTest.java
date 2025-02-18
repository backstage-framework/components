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

package com.backstage.app.dict.service;

import com.backstage.app.attachment.model.domain.Attachment;
import com.backstage.app.attachment.service.AttachmentService;
import com.backstage.app.dict.api.domain.DictFieldType;
import com.backstage.app.dict.common.CommonTest;
import com.backstage.app.dict.domain.Dict;
import com.backstage.app.dict.domain.DictField;
import com.backstage.app.dict.model.dictitem.DictDataItem;
import com.backstage.app.dict.service.advice.AttachmentDictDataServiceAdvice;
import com.backstage.app.model.other.user.UserInfo;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.backstage.app.dict.constant.ServiceFieldConstants.getServiceSchemeFields;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CommonDictAttachmentTest extends CommonTest
{
	@Autowired
	private AttachmentService attachmentService;

	@Value("classpath:attachment.png")
	protected Resource firstFileResource;

	@Value("classpath:attachment2.png")
	protected Resource secondFileResource;

	@Value("classpath:attachment3.png")
	protected Resource thirdFileResource;

	protected String firstAttachmentId;
	protected String secondAttachmentId;
	protected String thirdAttachmentId;

	protected Attachment firstAttachment;
	protected Attachment secondAttachment;
	protected Attachment thirdAttachment;

	protected static String TESTABLE_ATTACH_DICT_ID;

	@BeforeAll
	public void setupAttachment() throws IOException
	{
		firstAttachment = createAttachment(firstFileResource);
		secondAttachment = createAttachment(secondFileResource);
		thirdAttachment = createAttachment(thirdFileResource);

		firstAttachmentId = firstAttachment.getId();
		secondAttachmentId = secondAttachment.getId();
		thirdAttachmentId = thirdAttachment.getId();
	}

	protected void initTestableHierarchy(String storageDictId)
	{
		var attachDict = buildAttachmentDict(storageDictId);

		TESTABLE_ATTACH_DICT_ID = dictService.create(attachDict).getId();
	}

	protected void attachmentBindingWithCreateDictItem()
	{
		var attachmentDataMap = Map.of(
				"stringField", "string",
				"integerField", 1L,
				"doubleField", 2,
				"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T08:00:00.000Z"),
				"attachmentField", firstAttachmentId,
				"attachmentsField", List.of(firstAttachmentId),
				"booleanField", true);

		var dictItem = dictDataService.create(buildDictDataItem(TESTABLE_ATTACH_DICT_ID, attachmentDataMap));

		var singleFieldAttachments = attachmentService.getAttachments(AttachmentDictDataServiceAdvice.DICT_ITEM_ATTACHMENT_TYPE, AttachmentDictDataServiceAdvice.getAttachmentOwnerId(TESTABLE_ATTACH_DICT_ID, dictItem));
		var multivaluedFieldAttachments = attachmentService.getAttachments(AttachmentDictDataServiceAdvice.DICT_ITEM_ATTACHMENT_TYPE, AttachmentDictDataServiceAdvice.getAttachmentOwnerId(TESTABLE_ATTACH_DICT_ID, dictItem));

		assertNotNull(dictItem);
		assertEquals(firstAttachment.getId(), singleFieldAttachments.get(0).getId());
		assertEquals(firstAttachment.getId(), multivaluedFieldAttachments.get(0).getId());
	}

	protected void checkAttachmentBindingWithUpdateDictItem()
	{
		var attachmentDataMap = Map.of(
				"stringField", "string",
				"integerField", 1L,
				"doubleField", 2,
				"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T08:00:00.000Z"),
				"attachmentsField", List.of(secondAttachmentId, thirdAttachmentId),
				"booleanField", true);

		var attachmentDataUpdateMap = Map.of(
				"stringField", "string",
				"integerField", 1L,
				"doubleField", 2,
				"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T08:00:00.000Z"),
				"attachmentField", secondAttachmentId,
				"attachmentsField", List.of(secondAttachmentId, thirdAttachmentId),
				"booleanField", false);

		var dictItem = dictDataService.create(buildDictDataItem(TESTABLE_ATTACH_DICT_ID, attachmentDataMap));
		var updatedDictItem = dictDataService.update(dictItem.getId(), buildDictDataItem(TESTABLE_ATTACH_DICT_ID, attachmentDataUpdateMap), dictItem.getVersion());

		var fieldAttachments = attachmentService.getAttachments(AttachmentDictDataServiceAdvice.DICT_ITEM_ATTACHMENT_TYPE, AttachmentDictDataServiceAdvice.getAttachmentOwnerId(TESTABLE_ATTACH_DICT_ID, updatedDictItem));
		var fieldAttachmentsIds = fieldAttachments.stream().map(Attachment::getId).toList();

		assertNotNull(dictItem);
		assertTrue(fieldAttachmentsIds.contains(secondAttachmentId));
		assertTrue(fieldAttachmentsIds.contains(thirdAttachmentId));
		assertEquals(2, fieldAttachmentsIds.size());
	}

	protected void checkAttachmentReleaseWithDeleteDictItem()
	{
		var attachmentDataMap = Map.of(
				"stringField", "string",
				"integerField", 1L,
				"doubleField", 2,
				"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T08:00:00.000Z"),
				"booleanField", true,
				"attachmentField", firstAttachmentId,
				"attachmentsField", List.of(firstAttachmentId));

		var dictItem = dictDataService.create(buildDictDataItem(TESTABLE_ATTACH_DICT_ID, attachmentDataMap));

		dictDataService.delete(TESTABLE_ATTACH_DICT_ID, dictItem.getId(), true, dictItem.getVersion());

		var attachmentsAfterDelete = attachmentService.getAttachments(AttachmentDictDataServiceAdvice.DICT_ITEM_ATTACHMENT_TYPE, dictItem.getId());

		assertTrue(attachmentsAfterDelete.isEmpty());
	}

	protected void checkAttachmentBindingWithDeleteDictItem()
	{
		var attachmentDataMap = Map.of(
				"stringField", "string",
				"integerField", 1L,
				"doubleField", 2,
				"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T08:00:00.000Z"),
				"attachmentField", firstAttachmentId,
				"attachmentsField", List.of(firstAttachmentId),
				"booleanField", true);

		var dictItem = dictDataService.create(buildDictDataItem(TESTABLE_ATTACH_DICT_ID, attachmentDataMap));

		dictDataService.delete(TESTABLE_ATTACH_DICT_ID, dictItem.getId(), true, dictItem.getVersion());

		var attachmentsAfterDelete = attachmentService.getAttachments(AttachmentDictDataServiceAdvice.DICT_ITEM_ATTACHMENT_TYPE, TESTABLE_ATTACH_DICT_ID + "_" + dictItem.getId());

		assertTrue(attachmentsAfterDelete.isEmpty());

		var deletedDictItem = dictDataService.getById(TESTABLE_ATTACH_DICT_ID, dictItem.getId());

		dictDataService.delete(TESTABLE_ATTACH_DICT_ID, dictItem.getId(), false, deletedDictItem.getVersion());

		var attachmentsAfterRestore = attachmentService.getAttachments(AttachmentDictDataServiceAdvice.DICT_ITEM_ATTACHMENT_TYPE, TESTABLE_ATTACH_DICT_ID + "_" + deletedDictItem.getId());

		assertEquals(1, attachmentsAfterRestore.size());
	}

	protected void checkAttachmentBindingsWithUpdateDict()
	{
		var dict = buildAttachmentDict("updatingAttachmentDict");
		dictService.create(dict);
		dict.setFields(withoutServiceFields(dict.getFields()));

		var dictId = dict.getId();

		var attachmentDataMap1 = Map.of(
				"stringField", "string",
				"integerField", 1L,
				"doubleField", 2,
				"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T08:00:00.000Z"),
				"attachmentField", firstAttachmentId,
				"attachmentsField", List.of(secondAttachmentId),
				"booleanField", true);

		var dictItem1 = dictDataService.create(buildDictDataItem(dictId, attachmentDataMap1));

		var attachmentDataMap2 = Map.of(
				"stringField", "string",
				"integerField", 1L,
				"doubleField", 2,
				"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T08:00:00.000Z"),
				"attachmentField", secondAttachmentId,
				"attachmentsField", List.of(secondAttachmentId, thirdAttachmentId),
				"booleanField", true);

		var dictItem2 = dictDataService.create(buildDictDataItem(dictId, attachmentDataMap2));

		var objectIds = List.of(
				dictId + "_" + dictItem1.getId(),
				dictId + "_" + dictItem2.getId());

		var addedAttachments = attachmentService.getAttachments(
				AttachmentDictDataServiceAdvice.DICT_ITEM_ATTACHMENT_TYPE, objectIds);

		dict.getFields().removeIf(field -> field.getId().equals("attachmentsField"));
		dictService.update(dictId, dict);

		var updatedAttachments = attachmentService.getAttachments(
				AttachmentDictDataServiceAdvice.DICT_ITEM_ATTACHMENT_TYPE, objectIds);

		assertEquals(2, addedAttachments.size());
		assertEquals(2, updatedAttachments.size());
	}

	protected void checkAttachmentBindingsWithDeleteDict()
	{
		var dict = buildAttachmentDict("deletingAttachmentDict");
		var dictId = dictService.create(dict).getId();

		var attachmentDataMap1 = Map.of(
				"stringField", "string",
				"integerField", 1L,
				"doubleField", 2,
				"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T08:00:00.000Z"),
				"attachmentField", firstAttachmentId,
				"attachmentsField", List.of(secondAttachmentId),
				"booleanField", true);

		var dictItem1 = dictDataService.create(buildDictDataItem(dictId, attachmentDataMap1));

		var attachmentDataMap2 = Map.of(
				"stringField", "string",
				"integerField", 1L,
				"doubleField", 2,
				"timestampField", List.of("2021-08-15T06:00:00.000Z", "2021-08-15T08:00:00.000Z"),
				"attachmentField", secondAttachmentId,
				"attachmentsField", List.of(secondAttachmentId, thirdAttachmentId),
				"booleanField", true);

		var dictItem2 = dictDataService.create(buildDictDataItem(dictId, attachmentDataMap2));

		var objectIds = List.of(
				dictId + "_" + dictItem1.getId(),
				dictId + "_" + dictItem2.getId());

		var addedAttachments = attachmentService.getAttachments(
				AttachmentDictDataServiceAdvice.DICT_ITEM_ATTACHMENT_TYPE, objectIds);

		dictService.delete(dictId);

		var deletedAttachments = attachmentService.getAttachments(
				AttachmentDictDataServiceAdvice.DICT_ITEM_ATTACHMENT_TYPE, objectIds);

		assertEquals(2, addedAttachments.size());
		assertEquals(0, deletedAttachments.size());
	}

	private Attachment createAttachment(Resource fileResource) throws IOException
	{
		var bytes = IOUtils.toByteArray(fileResource.getInputStream());
		var attachment = attachmentService.addAttachment(Objects.requireNonNull(fileResource.getFilename()), MediaType.IMAGE_PNG_VALUE, UserInfo.SYSTEM_USER_ID, bytes);
		var savedData = attachmentService.getAttachmentData(attachment.getId());

		assertEquals(attachment.getSize(), bytes.length);
		assertArrayEquals(bytes, IOUtils.toByteArray(savedData.getInputStream()));

		return attachment;
	}

	protected DictDataItem buildDictDataItem(String dictId, Map<String, Object> dataMap)
	{
		return DictDataItem.of(dictId, dataMap);
	}

	private Dict buildAttachmentDict(String storageDictId)
	{
		var dict = buildDict(storageDictId + "dataAttach");

		dict.getFields()
				.add(DictField.builder()
						.id("attachmentField")
						.name("Вложение")
						.type(DictFieldType.ATTACHMENT)
						.required(false)
						.multivalued(false)
						.build());

		dict.getFields()
				.add(DictField.builder()
						.id("attachmentsField")
						.name("Вложения")
						.type(DictFieldType.ATTACHMENT)
						.required(false)
						.multivalued(true)
						.build());

		return dict;
	}

	protected List<DictField> withoutServiceFields(List<DictField> source)
	{
		return source.stream()
				.filter(it -> !getServiceSchemeFields().contains(it.getId()))
				.collect(Collectors.toList());
	}
}
