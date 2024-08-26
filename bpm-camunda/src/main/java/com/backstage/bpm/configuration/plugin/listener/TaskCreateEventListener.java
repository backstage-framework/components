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

import com.backstage.bpm.repository.ProcessRepository;
import com.backstage.bpm.service.CamundaProcessEngine;
import com.backstage.bpm.service.task.TaskAdapter;
import com.backstage.bpm.service.task.TaskManager;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.task.IdentityLink;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class TaskCreateEventListener implements TaskListener
{
	private final ProcessRepository processRepository;

	private final TaskManager taskManager;

	@Override
	public void notify(DelegateTask delegateTask)
	{
		var taskParameters = new HashMap<>(delegateTask.getVariables());

		taskParameters.putAll(Map.of(
				TaskAdapter.TaskProperties.TYPE, delegateTask.getName(),
				TaskAdapter.TaskProperties.NAME, delegateTask.getName(),
				TaskAdapter.TaskProperties.USER_ID, delegateTask.getCandidates().stream().map(IdentityLink::getUserId).filter(Objects::nonNull).collect(Collectors.joining(",")),
				TaskAdapter.TaskProperties.USER_ROLE, delegateTask.getCandidates().stream().map(IdentityLink::getGroupId).filter(Objects::nonNull).collect(Collectors.joining(","))
		));

		if (delegateTask.getVariable("taskType") != null)
		{
			taskParameters.put(TaskAdapter.TaskProperties.TYPE, delegateTask.getVariable("taskType"));
		}

		if (delegateTask.getDescription() != null)
		{
			taskParameters.put(TaskAdapter.TaskProperties.COMMENT, delegateTask.getDescription());
		}

		taskManager.createTask(TaskAdapter.createTask(processRepository.findByIdEx(delegateTask.getVariable(CamundaProcessEngine.PROCESS_PARAM_ID).toString()), delegateTask.getId(), taskParameters));
	}
}
