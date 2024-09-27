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

package com.backstage.app.attachment.endpoint;

import com.backstage.app.attachment.AbstractTests;
import com.google.common.io.ByteSource;
import com.jayway.jsonpath.JsonPath;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AttachmentEndpoint.class)
@ImportAutoConfiguration(exclude = SecurityAutoConfiguration.class)
public abstract class AbstractEndpointTests extends AbstractTests
{
	@Autowired
	private MockMvc mvc;

	@Value("classpath:attachment.png")
	private Resource fileResource;

	private static String attachmentId;

	@Order(1)
	@Test
	public void addAttachment() throws Exception
	{
		var bytes = IOUtils.toByteArray(fileResource.getInputStream());

		var result = mvc.perform(post("/api/attachment/upload")
						.param("fileName", fileResource.getFilename())
						.contentType(MediaType.IMAGE_PNG_VALUE)
						.content(bytes))
				.andExpect(status().isOk())
				.andReturn();

		attachmentId = JsonPath.read(result.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data");

		assertNotNull(attachmentId);
	}

	@Order(2)
	@Test
	public void getAttachment() throws Exception
	{
		var result = mvc.perform(get("/api/attachment/get")
						.param("id", attachmentId))
				.andExpect(status().isOk())
				.andReturn();

		var bytes = result.getResponse().getContentAsByteArray();
		var originalBytes = IOUtils.toByteArray(fileResource.getInputStream());

		assertArrayEquals(bytes, originalBytes);
	}

	@Order(3)
	@Test
	public void getPartialAttachment() throws Exception
	{
		attachmentRangeTest(0, 100L);
		attachmentRangeTest(100, 100L);
		attachmentRangeTest(100, null);
		attachmentRangeTest(0, null);
	}

	private void attachmentRangeTest(long offset, Long length) throws Exception
	{
		var result = mvc.perform(get("/api/attachment/get")
						.param("id", attachmentId)
						.header("Range", buildRangeHeader(offset, length)))
				.andExpect(header().exists("Content-Range"))
				.andExpect(header().exists("Content-Length"))
				.andExpect(status().isPartialContent())
				.andReturn();

		var bytes = result.getResponse().getContentAsByteArray();

		var originalBytes = IOUtils.toByteArray(fileResource.getInputStream());
		var originalBytesRanged = ByteSource.wrap(originalBytes).slice(offset, length != null ? length : originalBytes.length - offset).read();

		assertArrayEquals(bytes, originalBytesRanged);
	}

	private String buildRangeHeader(long offset, Long length)
	{
		if (length != null)
		{
			length = offset + length - 1;
		}

		return "bytes=%d-%s".formatted(offset, Objects.toString(length, ""));
	}
}
