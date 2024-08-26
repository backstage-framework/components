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
import com.backstage.bpm.repository.ProcessRepository;
import com.backstage.bpm.service.task.TaskManager;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.impl.bpmn.behavior.UserTaskActivityBehavior;
import org.camunda.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener;
import org.camunda.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;
import org.camunda.bpm.engine.impl.pvm.process.ScopeImpl;
import org.camunda.bpm.engine.impl.task.TaskDefinition;
import org.camunda.bpm.engine.impl.util.xml.Element;
import org.camunda.bpm.model.bpmn.impl.BpmnModelConstants;

@RequiredArgsConstructor
public class CustomBpmnParseListener extends AbstractBpmnParseListener
{
	private ProcessStartExecutionListener processStartExecutionListener;

	private TaskListener taskCreateEventListener;
	private TaskListener taskDeleteEventListener;

	private TerminateEndEventActivityBehavior terminateEndEventActivityBehavior;

	public CustomBpmnParseListener(ProcessRepository processRepository, TaskManager taskManager, BpmProperties bpmProperties)
	{
		processStartExecutionListener = new ProcessStartExecutionListener(processRepository);

		taskCreateEventListener = new TaskCreateEventListener(processRepository, taskManager);
		taskDeleteEventListener = new TaskDeleteEventListener(taskManager);

		terminateEndEventActivityBehavior = new TerminateEndEventActivityBehavior(bpmProperties);
	}

	@Override
	public void parseProcess(Element processElement, ProcessDefinitionEntity processDefinition)
	{
		processDefinition.addListener(ExecutionListener.EVENTNAME_START, processStartExecutionListener);
	}

	@Override
	public void parseEndEvent(Element endEventElement, ScopeImpl scope, ActivityImpl activity)
	{
		if (endEventElement.element(BpmnModelConstants.BPMN_ELEMENT_TERMINATE_EVENT_DEFINITION) != null)
		{
			activity.setActivityBehavior(terminateEndEventActivityBehavior);
		}
	}

	@Override
	public void parseUserTask(Element userTaskElement, ScopeImpl scope, ActivityImpl activity)
	{
		TaskDefinition taskDefinition = ((UserTaskActivityBehavior) activity.getActivityBehavior()).getTaskDefinition();

		taskDefinition.addBuiltInTaskListener(TaskListener.EVENTNAME_CREATE, taskCreateEventListener);
		taskDefinition.addBuiltInTaskListener(TaskListener.EVENTNAME_DELETE, taskDeleteEventListener);
	}
}
