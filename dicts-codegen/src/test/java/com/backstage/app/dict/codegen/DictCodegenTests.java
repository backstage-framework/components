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

package com.backstage.app.dict.codegen;

import com.backstage.app.api.model.ApiResponse;
import com.backstage.app.dict.api.domain.DictFieldType;
import com.backstage.app.dict.api.model.dto.DictDto;
import com.backstage.app.dict.api.model.dto.DictEnumDto;
import com.backstage.app.dict.api.model.dto.DictFieldDto;
import com.backstage.app.dict.api.service.remote.RemoteDictService;
import com.backstage.app.dict.conversion.dto.DictConverter;
import com.backstage.app.dict.domain.Dict;
import com.backstage.app.dict.service.DictService;
import com.backstage.app.dict.service.codegen.client.ClientCodegenExtension;
import com.backstage.app.dict.service.codegen.server.ServerCodegenExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DictCodegenTests
{
	private static final FileTime ORIGINAL_TIME = FileTime.from(Instant.parse("2020-01-01T00:00:00Z"));

	@Test
	void rewritesOnlyChangedRemoteSources(@TempDir Path directory) throws IOException
	{
		var context = mock(ApplicationContext.class);
		var service = mock(RemoteDictService.class);
		when(context.getBean(RemoteDictService.class)).thenReturn(service);
		when(service.list()).thenReturn(ApiResponse.of(List.of(dict(1L, "blue", "red"))));

		var codegen = new ClientCodegenExtension(context, directory.toString(), "example.generated");
		codegen.generate();

		var modelSource = directory.resolve("example/generated/ColorsDictItem.java");
		var serviceSource = directory.resolve("example/generated/ColorsDictItemService.java");
		var originalModel = Files.readString(modelSource);
		var originalService = Files.readString(serviceSource);
		Files.setLastModifiedTime(modelSource, ORIGINAL_TIME);
		Files.setLastModifiedTime(serviceSource, ORIGINAL_TIME);

		when(service.list()).thenReturn(ApiResponse.of(List.of(dict(1L, "red", "blue"))));
		codegen.generate();

		assertEquals(originalModel, Files.readString(modelSource));
		assertEquals(originalService, Files.readString(serviceSource));
		assertEquals(ORIGINAL_TIME, Files.getLastModifiedTime(modelSource));
		assertEquals(ORIGINAL_TIME, Files.getLastModifiedTime(serviceSource));

		when(service.list()).thenReturn(ApiResponse.of(List.of(dict(2L, "red", "blue", "green"))));
		codegen.generate();

		assertTrue(Files.readString(modelSource).contains("GREEN(\"green\")"));
		assertNotEquals(ORIGINAL_TIME, Files.getLastModifiedTime(modelSource));
		assertEquals(ORIGINAL_TIME, Files.getLastModifiedTime(serviceSource));
	}

	@Test
	void keepsUnchangedNativeSources(@TempDir Path directory) throws IOException
	{
		var context = mock(ApplicationContext.class);
		var service = mock(DictService.class);
		var converter = mock(DictConverter.class);
		var schema = new Dict();
		when(context.getBean(DictService.class)).thenReturn(service);
		when(context.getBean(DictConverter.class)).thenReturn(converter);
		when(service.getAll()).thenReturn(List.of(schema));
		when(converter.convert(schema)).thenReturn(dict(1L, "blue", "red"));

		var codegen = new ServerCodegenExtension(context, directory.toString(), "example.generated");
		codegen.generate();

		var generatedDirectory = directory.resolve("example/generated");
		var sources = List.of(
				generatedDirectory.resolve("ColorsDictItem.java"),
				generatedDirectory.resolve("ColorsDictItemService.java"),
				generatedDirectory.resolve("ColorsDictItemAbstractAdvice.java"));
		for (var source : sources)
		{
			Files.setLastModifiedTime(source, ORIGINAL_TIME);
		}

		codegen.generate();

		for (var source : sources)
		{
			assertEquals(ORIGINAL_TIME, Files.getLastModifiedTime(source), source.toString());
		}
	}

	private DictDto dict(long version, String... colors)
	{
		return DictDto.builder()
				.id("colors")
				.version(version)
				.fields(List.of(
						DictFieldDto.builder().id("id").type(DictFieldType.STRING).build(),
						DictFieldDto.builder().id("version").type(DictFieldType.INTEGER).build()))
				.enums(List.of(DictEnumDto.builder()
						.id("color")
						.values(new LinkedHashSet<>(List.of(colors)))
						.build()))
				.build();
	}
}
