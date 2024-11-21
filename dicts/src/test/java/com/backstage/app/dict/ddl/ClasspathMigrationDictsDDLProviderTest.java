package com.backstage.app.dict.ddl;

import com.backstage.app.dict.common.CommonTest;
import com.backstage.app.dict.common.TestPipeline;
import com.backstage.app.dict.configuration.ddl.DictsDDLProvider;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Order(TestPipeline.FIRST_INIT)
public class ClasspathMigrationDictsDDLProviderTest extends CommonTest
{
	public static final String USERS_DICT_NAME = "users";

	@Autowired
	private DictsDDLProvider dictsDDLProvider;

	@Test
	void updateCorrect()
	{
		dictsDDLProvider.update();

		assertEquals(USERS_DICT_NAME, dictService.getById(USERS_DICT_NAME).getId());
	}
}
