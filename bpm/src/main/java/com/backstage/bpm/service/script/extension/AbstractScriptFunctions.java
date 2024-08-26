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

package com.backstage.bpm.service.script.extension;

import com.backstage.bpm.domain.Process;
import com.backstage.bpm.exception.BpmException;
import com.backstage.bpm.service.script.ScriptEngine;
import com.backstage.bpm.service.workflow.WorkflowService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractScriptFunctions extends SpringAwareScriptingExtension
{
	private static final String DEFAULT_BINDING_NAME = "scriptFunctions";

	private final WorkflowService workflowService;
	private final ScriptEngine scriptEngine;

	@PostConstruct
	public void initialize()
	{
		setBindingName(DEFAULT_BINDING_NAME);
	}

	protected Object executeScript(Process process, String scriptName, Object... args)
	{
		log.info("Executing script '{}'.", scriptName);

		var script = workflowService.getWorkflowScript(process.getWorkflowId(), scriptName + "Script");

		if (script == null)
		{
			throw new BpmException(String.format("failed to find process script '%s'", scriptName));
		}

		var scriptArgs = new ArrayList<>(args.length + 1);
		scriptArgs.add(process);
		scriptArgs.addAll(Arrays.asList(args));

		return scriptEngine.execute(script, "execute", scriptArgs.toArray());
	}
}
