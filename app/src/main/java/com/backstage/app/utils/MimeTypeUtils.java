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

package com.backstage.app.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@UtilityClass
public class MimeTypeUtils
{
	private final Map<String, String> ext2mime;

	static
	{
		HashMap<String, String> e2m = new HashMap<>();
		e2m.put("jpg", "image/jpeg");
		e2m.put("txt", "text/plain");
		e2m.put("png", "image/png");
		e2m.put("pdf", "application/pdf");
		e2m.put("bmp", "image/bmp");
		e2m.put("doc", "application/msword");
		e2m.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
		e2m.put("xls", "application/vnd.ms-excel");
		e2m.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		e2m.put("csv", "text/csv");
		e2m.put("json", "application/json");

		ext2mime = Map.copyOf(e2m);
	}

	// TODO: доработать с использованием ext2mime
	public String getMimeTypeByFilename(String filename)
	{
		String result = null;

		try
		{
			result = Files.probeContentType(Path.of(filename));
		}
		catch (Exception e)
		{
			log.warn("Failed to detect mime type by filename '%s'".formatted(filename), e);
		}

		if (result == null)
		{
			return MediaType.ALL_VALUE;
		}

		return result;
	}

	public String extensionToMimeType(String mimeType)
	{
		return ext2mime.get(mimeType.toLowerCase());
	}
}
