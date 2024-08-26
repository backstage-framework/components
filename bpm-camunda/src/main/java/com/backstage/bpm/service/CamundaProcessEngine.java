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

package com.backstage.bpm.service;

import com.backstage.bpm.configuration.plugin.listener.ProcessStartExecutionListener;
import com.backstage.bpm.domain.Process;
import com.backstage.bpm.domain.Task;
import com.backstage.bpm.model.CamundaProcessContext;
import com.backstage.bpm.model.EngineType;
import com.backstage.bpm.model.ProcessTimer;
import com.backstage.bpm.model.Workflow;
import com.backstage.bpm.repository.ProcessRepository;
import com.backstage.bpm.repository.TaskRepository;
import com.backstage.bpm.service.process.AbstractProcessEngine;
import com.backstage.bpm.service.script.ScriptEngine;
import com.backstage.bpm.service.task.TaskManager;
import com.backstage.bpm.service.workflow.WorkflowService;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.cmd.SetProcessDefinitionVersionCmd;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.Execution;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Predicate;

@Slf4j
@Service
@Transactional
public class CamundaProcessEngine extends AbstractProcessEngine<CamundaProcessContext>
{
	public static final String PROCESS_PARAM_ID = "__processId";

	private final org.camunda.bpm.engine.ProcessEngine processEngine;

	public CamundaProcessEngine(TaskRepository taskRepository,
	                            ProcessRepository processRepository,
	                            TaskManager taskManager,
	                            WorkflowService workflowService,
	                            ScriptEngine scriptEngine,
	                            ProcessEngine processEngine)
	{
		super(taskRepository, processRepository, taskManager, workflowService, scriptEngine, EngineType.CAMUNDA);

		this.processEngine = processEngine;
	}

	@Override
	protected CamundaProcessContext getProcessContext(Process process)
	{
		return new CamundaProcessContext(process);
	}

	@Override
	protected boolean isProcessContextActive(CamundaProcessContext processContext)
	{
		return !Optional.ofNullable(processEngine.getRuntimeService()
						.createProcessInstanceQuery()
						.processInstanceId(processContext.getProcess().getInstanceId())
						.singleResult())
				.map(Execution::isEnded)
				.orElse(true);
	}

	@Override
	protected void startProcessInternal(CamundaProcessContext processContext)
	{
		var process = processContext.getProcess();
		var parameters = new HashMap<>(processContext.getProcess().getParameters());
		parameters.put(PROCESS_PARAM_ID, process.getId());

		var workflow = getWorkflowService().getWorkflow(process.getWorkflowId());
		var processDefinition = getProcessDefinition(workflow);

		var processInstanceBuilder = processEngine.getRuntimeService().createProcessInstanceById(processDefinition.getId());
		processInstanceBuilder.setVariables(parameters);
		processInstanceBuilder.execute();
	}

	@Override
	protected void startProcessOnEventInternal(CamundaProcessContext processContext, String event, Map<String, Object> eventParameters, Runnable beforeStartTrigger)
	{
		try
		{
			var process = processContext.getProcess();
			var parameters = new HashMap<>(processContext.getProcess().getParameters());
			parameters.put(PROCESS_PARAM_ID, process.getId());

			ProcessStartExecutionListener.registerCallback(delegateExecution -> {
				beforeStartTrigger.run();

				var definition = processEngine.getRepositoryService().createProcessDefinitionQuery().processDefinitionId(delegateExecution.getProcessDefinitionId()).singleResult();
				var deployment = processEngine.getRepositoryService().createDeploymentQuery().deploymentId(definition.getDeploymentId()).singleResult();

				process.setWorkflowId(deployment.getName());
			});

			processEngine.getRuntimeService().signalEventReceived(event, parameters);
		}
		finally
		{
			ProcessStartExecutionListener.unregisterCallback();
		}
	}

	@Override
	protected void stopProcessInternal(CamundaProcessContext processContext)
	{
		processEngine.getRuntimeService().deleteProcessInstance(processContext.getProcess().getInstanceId(), "");
	}

	@Override
	public void killProcess(String processId)
	{
		stopProcess(processId);
	}

	@Override
	protected void completeTaskInternal(Task task, Map<String, Object> parameters)
	{
		processEngine.getTaskService().complete(task.getWorkItemId(), parameters);
	}

	@Override
	protected void cancelTaskInternal(Task task)
	{
		processEngine.getTaskService().deleteTask(task.getWorkItemId());
	}

	@Override
	protected void sendEventInternal(CamundaProcessContext processContext, String event, Map<String, Object> eventParameters)
	{
		var signalEvent = processEngine.getRuntimeService()
				.createEventSubscriptionQuery()
				.eventName(event)
				.processInstanceId(processContext.getProcess().getInstanceId())
				.singleResult();

		if (signalEvent != null)
		{
			processEngine.getRuntimeService()
					.signalEventReceived(signalEvent.getEventName(), signalEvent.getExecutionId(), eventParameters);
		}
		else
		{
			var signalExists = !processEngine.getRuntimeService()
					.createExecutionQuery()
					.signalEventSubscriptionName(event)
					.list()
					.isEmpty();

			if (signalExists)
			{
				processEngine.getRuntimeService().createExecutionQuery().processInstanceId(processContext.getProcess().getInstanceId())
						.list()
						.stream()
						.filter(Predicate.not(Execution::isEnded))
						.map(ExecutionEntity.class::cast)
						.filter(it -> it.getActivityId() != null)
						.forEach(execution -> processEngine.getRuntimeService().signal(execution.getId(), event, eventParameters, Map.of()));
			}
		}
	}

	@Override
	protected void updateProcessParametersInternal(CamundaProcessContext processContext)
	{
		processEngine.getRuntimeService().setVariables(processContext.getProcess().getInstanceId(), processContext.getProcess().getParameters());
	}

	@Override
	protected void migrateWorkflowInternal(CamundaProcessContext processContext, Workflow targetWorkflow)
	{
		var processDefinition = getProcessDefinition(targetWorkflow);
		var command = new SetProcessDefinitionVersionCmd(processContext.getProcess().getInstanceId(), processDefinition.getVersion());

		((ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration()).getCommandExecutorTxRequired().execute(command);
	}

	@Override
	public List<ProcessTimer> getProcessTimers(String processId)
	{
		var process = getProcessRepository().findByIdEx(processId);

		return processEngine.getManagementService().createJobQuery()
				.processInstanceId(process.getInstanceId())
				.active()
				.timers()
				.list()
				.stream()
				.map(job -> new ProcessTimer(job.getId(), job.getDuedate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()))
				.toList();
	}

	@Override
	public void updateProcessTimer(String processId, String timerName, LocalDateTime nextFireTime)
	{
		processEngine.getManagementService().setJobDuedate(timerName, Date.from(nextFireTime.atZone(ZoneId.systemDefault()).toInstant()));
	}

	private ProcessDefinition getProcessDefinition(Workflow workflow)
	{
		return processEngine.getRepositoryService().createProcessDefinitionQuery()
				.processDefinitionKey(workflow.getId())
				.versionTag(workflow.getVersion().toString())
				.singleResult();
	}
}
