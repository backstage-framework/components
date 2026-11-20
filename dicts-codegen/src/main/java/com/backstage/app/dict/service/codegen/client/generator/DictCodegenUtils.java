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

package com.backstage.app.dict.service.codegen.client.generator;

import jakarta.annotation.Generated;
import lombok.experimental.UtilityClass;
import org.springframework.javapoet.AnnotationSpec;
import org.springframework.javapoet.JavaFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

@UtilityClass
public class DictCodegenUtils
{
	private final String DEFAULT_SUPPRESS_WARNINGS_VALUE = "unchecked";

	public AnnotationSpec generatedAnnotation(Object generator)
	{
		return AnnotationSpec.builder(Generated.class)
				.addMember("value", "$S", generator.getClass().getName())
				.build();
	}

	public AnnotationSpec suppressWarningsAnnotation()
	{
		return suppressWarningsAnnotation(DEFAULT_SUPPRESS_WARNINGS_VALUE);
	}

	public AnnotationSpec suppressWarningsAnnotation(String value)
	{
		return AnnotationSpec.builder(SuppressWarnings.class)
				.addMember("value", "$S", value)
				.build();
	}

	public void writeIfChanged(JavaFile source, Path directory) throws IOException
	{
		var outputDirectory = directory.resolve(source.packageName().replace('.', '/'));
		var target = outputDirectory.resolve(source.typeSpec().name() + ".java");
		var content = source.toString().getBytes(StandardCharsets.UTF_8);

		try
		{
			if (Arrays.equals(Files.readAllBytes(target), content))
			{
				return;
			}
		}
		catch (NoSuchFileException ignored)
		{
			// Файл нужно создать.
		}

		Files.createDirectories(outputDirectory);
		var temporary = Files.createTempFile(outputDirectory, ".codegen-", ".tmp");

		try
		{
			Files.write(temporary, content);

			try
			{
				Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			}
			catch (AtomicMoveNotSupportedException ignored)
			{
				Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
			}
		}
		finally
		{
			Files.deleteIfExists(temporary);
		}
	}
}
