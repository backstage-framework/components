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

package com.backstage.bpm.configuration.plugin;

import com.backstage.bpm.configuration.plugin.listener.CustomBpmnParseListener;
import com.backstage.bpm.configuration.plugin.listener.ScriptingExtensionResolver;
import com.backstage.bpm.configuration.properties.BpmProperties;
import com.backstage.bpm.repository.ProcessRepository;
import com.backstage.bpm.service.script.extension.ScriptingExtensionLocator;
import com.backstage.bpm.service.task.TaskManager;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.spring.SpringProcessEngineConfiguration;
import org.camunda.bpm.spring.boot.starter.util.SpringBootProcessEnginePlugin;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BootstrapPlugin extends SpringBootProcessEnginePlugin
{
	private final ProcessRepository processRepository;

	private final TaskManager taskManager;
	private final ScriptingExtensionLocator scriptingExtensionLocator;

	private final BpmProperties bpmProperties;

	@Override
	public void preInit(SpringProcessEngineConfiguration processEngineConfiguration)
	{
		var scheme = bpmProperties.getDdl()
				.getScheme();

		if (scheme != null)
		{
			processEngineConfiguration.setDatabaseSchema(scheme);
			processEngineConfiguration.setDatabaseTablePrefix(scheme + ".");
		}

		processEngineConfiguration.getCustomPostBPMNParseListeners().add(new CustomBpmnParseListener(processRepository, taskManager, bpmProperties));

		processEngineConfiguration.setHistoryCleanupEnabled(true);
		processEngineConfiguration.setHistoryTimeToLive("P1D");
		processEngineConfiguration.setHistoryCleanupBatchWindowStartTime("02:00");
		processEngineConfiguration.setHistoryCleanupBatchWindowStartTime("06:00");
	}

	@Override
	public void postInit(ProcessEngineConfigurationImpl processEngineConfiguration)
	{
		processEngineConfiguration.getResolverFactories().add(new ScriptingExtensionResolver(scriptingExtensionLocator));
	}
}
