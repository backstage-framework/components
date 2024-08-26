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

package com.backstage.bpm.configuration.plugin.listener;

import com.backstage.bpm.configuration.properties.BpmProperties;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.impl.bpmn.behavior.FlowNodeActivityBehavior;
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution;

@RequiredArgsConstructor
public class TerminateEndEventActivityBehavior extends FlowNodeActivityBehavior
{
	private final BpmProperties bpmProperties;

	public void execute(ActivityExecution execution)
	{
		execution.end(true);

		if (bpmProperties.getDefaultTerminatingEndEventScope() == BpmProperties.TerminatingEndEventScope.PROCESS)
		{
			execution.getProcessEngine().getRuntimeService().deleteProcessInstance(execution.getProcessInstanceId(), "terminated", false, true, true);
		}
	}
}
