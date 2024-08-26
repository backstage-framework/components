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

import com.backstage.bpm.exception.BpmException;
import com.backstage.bpm.repository.ProcessRepository;
import com.backstage.bpm.service.CamundaProcessEngine;
import com.backstage.bpm.service.process.AbstractProcessEngine;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;

import java.util.function.Consumer;

@RequiredArgsConstructor
public class ProcessStartExecutionListener implements ExecutionListener
{
	private final ProcessRepository processRepository;

	private static final ThreadLocal<Consumer<DelegateExecution>> callback = new ThreadLocal<>();

	@Override
	public void notify(DelegateExecution execution)
	{
		// TODO: обрабатывать дочерние процессы
		if (callback.get() != null)
		{
			callback.get().accept(execution);
		}

		var process = processRepository.findByIdEx(execution.getProcessInstance().getVariable(CamundaProcessEngine.PROCESS_PARAM_ID).toString());

		if (!AbstractProcessEngine.UNDEFINED_INSTANCE_ID.equals(process.getInstanceId()))
		{
			throw new BpmException("trying to start more than one process instance in one execution");
		}

		process.setInstanceId(execution.getProcessInstanceId());

		processRepository.saveAndFlush(process);
	}

	public static void registerCallback(Consumer<DelegateExecution> executionConsumer)
	{
		callback.set(executionConsumer);
	}

	public static void unregisterCallback()
	{
		callback.remove();
	}
}
