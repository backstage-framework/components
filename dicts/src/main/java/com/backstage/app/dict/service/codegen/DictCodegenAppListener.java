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

package com.backstage.app.dict.service.codegen;

import com.backstage.app.dict.conversion.dto.DictConverter;
import com.backstage.app.dict.service.DictService;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty("app.dicts.codegen.outputPath")
public class DictCodegenAppListener implements ApplicationContextAware, ApplicationListener<ContextRefreshedEvent>
{
	@Setter
	private ApplicationContext applicationContext;

	private final TaskExecutor taskExecutor;

	private final DictService dictService;
	private final DictConverter dictConverter;

	@Value("${app.dicts.codegen.outputPath}")
	private String outputPath;

	@Value("${app.dicts.codegen.targetPackage}")
	private String targetPackage;

	@Override
	public void onApplicationEvent(final ContextRefreshedEvent event)
	{
		taskExecutor.execute(() -> {
			log.info("Starting dicts API (native) client generation.");

			var modelGenerator = new DictCodegenExtension(dictService, dictConverter, outputPath, targetPackage);
			modelGenerator.generate();

			log.info("Done. Closing application.");

			SpringApplication.exit(applicationContext, () -> 0);
		});
	}
}
