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

package com.backstage.app.dict.service.mongo;

import com.backstage.app.dict.common.TestPipeline;
import com.backstage.app.dict.service.CommonDictAttachmentTest;
import org.junit.jupiter.api.*;

@Order(TestPipeline.MONGO_DICT_ATTACHMENT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@MongoStorage
public class MongoDictAttachmentTest extends CommonDictAttachmentTest
{
	@BeforeAll
	public void createMongoTestableHierarchy()
	{
		initTestableHierarchy(MONGO_DICT_ID);
	}

	@Test
	void check_attachmentBindingWithCreateDictItem()
	{
		attachmentBindingWithCreateDictItem();
	}

	@Test
	void check_attachmentBindingWithUpdateDictItem()
	{
		checkAttachmentBindingWithUpdateDictItem();
	}

	@Test
	void check_attachmentReleaseWithDeleteDictItem()
	{
		checkAttachmentReleaseWithDeleteDictItem();
	}

	@Test
	void check_attachmentBindingDeleteDictItem()
	{
		checkAttachmentBindingWithDeleteDictItem();
	}

	@Test
	void check_attachmentBindingsWithUpdateDict()
	{
		checkAttachmentBindingsWithUpdateDict();
	}

	@Test
	void check_attachmentBindingsWithDeleteDict()
	{
		checkAttachmentBindingsWithDeleteDict();
	}
}
