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

package com.backstage.bpm.service.script;

import com.backstage.bpm.exception.BpmException;
import com.backstage.bpm.model.EngineType;
import com.backstage.bpm.repository.ProcessRepository;
import com.backstage.bpm.service.script.extension.AbstractScriptFunctions;
import com.backstage.bpm.service.workflow.WorkflowService;
import org.camunda.bpm.engine.runtime.Execution;
import org.springframework.stereotype.Component;

@Component
public class CamundaScriptFunctions extends AbstractScriptFunctions
{
	private final ProcessRepository processRepository;

	public CamundaScriptFunctions(WorkflowService workflowService, ScriptEngine scriptEngine, ProcessRepository processRepository)
	{
		super(workflowService, scriptEngine);

		this.processRepository = processRepository;
	}

	public Object invokeScript(Execution processContext, String scriptName, Object... args)
	{
		var process = processRepository.findByInstanceId(processContext.getProcessInstanceId()).orElseThrow(() ->
			new BpmException("cannot find process for camunda process instance %s".formatted(processContext.getProcessInstanceId()))
		);

		return executeScript(process, scriptName, args);
	}

	@Override
	public boolean supports(EngineType engineType)
	{
		return EngineType.CAMUNDA.equals(engineType);
	}
}
