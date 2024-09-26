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

package com.backstage.app.attachment.service.store;

import com.backstage.app.utils.DateUtils;
import io.minio.GetObjectResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.AbstractResource;

import java.io.InputStream;
import java.time.LocalDateTime;

@RequiredArgsConstructor
public class MinioResource extends AbstractResource
{
	private final GetObjectResponse minioResponse;

	private final LocalDateTime lastModified;

	private final long contentLength;

	@Getter
	@Setter
	private String description;

	@Override
	public @NotNull InputStream getInputStream()
	{
		return minioResponse;
	}

	@Override
	public boolean exists()
	{
		return true;
	}

	@Override
	public boolean isOpen()
	{
		return true;
	}

	@Override
	public long contentLength()
	{
		return contentLength;
	}

	@Override
	public long lastModified()
	{
		return DateUtils.toEpochMilli(lastModified);
	}
}
