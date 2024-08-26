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

package com.backstage.app.dict.api.service.codegen;

import com.backstage.app.api.utils.RemoteServiceUtils;
import com.backstage.app.dict.api.service.remote.RemoteDictService;
import com.backstage.app.dict.api.service.codegen.generator.DictItemModelGenerator;
import com.backstage.app.dict.api.service.codegen.generator.DictItemServiceGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.javapoet.JavaFile;

import java.io.IOException;
import java.nio.file.Path;

@Slf4j
@RequiredArgsConstructor
public class DictApiCodegenExtension
{
	private final RemoteDictService dictService;

	private final DictItemModelGenerator modelGenerator = new DictItemModelGenerator();
	private final DictItemServiceGenerator serviceGenerator = new DictItemServiceGenerator();

	private final String outputPath;
	private final String outputPackage;

	protected void generate()
	{
		RemoteServiceUtils.executeAndGetData(dictService::list).forEach(dict -> {
			var model = JavaFile.builder(outputPackage, modelGenerator.generate(dict)).build();
			var service = JavaFile.builder(outputPackage, serviceGenerator.generate(model)).build();

			try
			{
				var path = Path.of(outputPath);

				model.writeToPath(path);
				service.writeToPath(path);
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		});
	}
}
