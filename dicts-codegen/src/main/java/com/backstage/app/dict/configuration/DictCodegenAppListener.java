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

package com.backstage.app.dict.configuration;

import com.backstage.app.dict.service.codegen.client.ClientCodegenExtension;
import com.backstage.app.dict.service.codegen.server.ServerCodegenExtension;
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
import org.springframework.core.task.SimpleAsyncTaskExecutor;
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

	private final TaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();

	@Value("${app.dicts.codegen.outputPath}")
	private String outputPath;

	@Value("${app.dicts.codegen.targetPackage}")
	private String targetPackage;

	@Override
	public void onApplicationEvent(final ContextRefreshedEvent event)
	{
		taskExecutor.execute(() -> {
			if (applicationContext.containsBean("dictService"))
			{
				log.info("Generating native dicts client.");

				var codegenExtension = new ServerCodegenExtension(applicationContext, outputPath, targetPackage);
				codegenExtension.generate();
			}
			else
			{
				log.info("Generating remote dicts client.");

				var codegenExtension = new ClientCodegenExtension(applicationContext, outputPath, targetPackage);
				codegenExtension.generate();
			}

			log.info("Done. Closing application.");

			SpringApplication.exit(applicationContext, () -> 0);
		});
	}
}
