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

package com.backstage.bpm;

import com.backstage.app.utils.TimeUtils;
import com.backstage.bpm.model.TaskFilterHelper;
import com.backstage.bpm.service.process.ProcessService;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Order(1)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CamundaBpmTests extends AbstractTests
{
	@Autowired private ProcessService processService;

	@Test
	void tasksAndSignals()
	{
		var paramName = "objectId";
		var paramValue = "1";

		var process = processService.startProcess("tasksAndSignals", Map.of(paramName, paramValue));
		var filter = TaskFilterHelper.pendingByRole(process, "ROLE_ADMIN");

		var task = processService.getTask(filter).orElse(null);

		// Проверки обработки множественных ролей и идентификаторов пользователей.
		assertNotNull(task);
		assertEquals(task.getType(), "Task1");
		assertEquals(task.getUserRoles().size(), 2);
		assertTrue(task.getUserRoles().contains("ROLE_ADMIN"));
		assertTrue(task.getUserRoles().contains("ROLE_USER"));
		assertEquals(task.getCandidateUserIds().size(), 2);
		assertTrue(task.getCandidateUserIds().contains("user1"));
		assertTrue(task.getCandidateUserIds().contains("user2"));
		assertEquals(task.getParameters().get(paramName), paramValue);

		processService.assignTask(task.getId(), "user1");
		processService.completeTask(task, "complete");

		// После выполнения задачи, должен завершиться embedded subprocess через отправку сигнала и проверку условия на развилке с отменой задачи Task2.
		process = processService.getProcess(process.getId()).orElse(null);

		assertNotNull(process);
		assertTrue((Boolean) process.getParameters().get("task2Canceled"));

		// После выхода из embedded subprocess создаётся Task3 с таймером на 5 секунд, дожидаемся её отмены.
		task = processService.getTask(filter).orElse(null);

		assertNotNull(task);

		TimeUtils.sleepSeconds(15);

		process = processService.getProcess(process.getId()).orElse(null);

		assertNotNull(process);
		assertTrue((Boolean) process.getParameters().get("task3Canceled"));

		// После отмены Task3 создаётся финальная задача, после которой процесс завершается.
		task = processService.getTask(filter).orElse(null);

		// Дополнительно проверяем маппинг ключевых атрибутов для Human Task.
		assertNotNull(task);
		assertEquals(task.getType(), "Task4");
		assertEquals(task.getName(), "Последняя задача");
		assertEquals(task.getComment(), "Задача, которая завершает процесс.");

		processService.completeTask(task, "complete");

		process = processService.getProcess(process.getId()).orElse(null);

		assertNotNull(process);
		assertFalse(process.isActive());
	}

	@Test
	void eventDrivenStart()
	{
		var paramName = "objectId";
		var resultParamName = "resultParam";
		var paramValue = 1;

		var process = processService.startProcessOnEvent("startSignal11", Map.of(paramName, paramValue), Map.of(paramName, paramValue));

		assertTrue(process.isPresent());
		assertTrue(process.get().isActive());
		assertEquals(process.get().getParameters().get(paramName), paramValue);
		assertEquals(process.get().getParameters().get(resultParamName), paramValue);

		process = processService.startProcessOnEvent("startSignal12", Map.of());

		assertTrue(process.isPresent());
		assertFalse(process.get().isActive());
		assertTrue(process.get().getParameters().isEmpty());

		process = processService.startProcessOnEvent("startSignal13", Map.of());

		assertFalse(process.isPresent());
	}

	@Test
	void terminatingSubProcessTest()
	{
		var process = processService.startProcess("terminatingSubProcess");

		assertFalse(process.isActive());
	}

	@Test
	void nonTerminatingSubProcessTest()
	{
		var process = processService.startProcess("nonTerminatingSubProcess");

		assertTrue(process.isActive());
	}

	@Test
	void stopProcessWithPendingTimerTest()
	{
		var process = processService.startProcess("boundaryTimer");

		assertFalse(processService.getProcessTimers(process.getId()).isEmpty());

		processService.stopProcess(process.getId());

		process = processService.getProcess(process.getId()).orElse(null);

		assertNotNull(process);
		assertFalse(process.isActive());
		assertTrue(processService.getProcessTimers(process.getId()).isEmpty());
	}

	@Test
	void boundaryTimerTest()
	{
		var process = processService.startProcess("boundaryTimer");
		var timers = processService.getProcessTimers(process.getId());

		assertFalse(timers.isEmpty());

		processService.sendEvent(process, "skipBoundaryTimer1");

		timers = processService.getProcessTimers(process.getId());

		assertFalse(timers.isEmpty());

		processService.sendEvent(process, "skipBoundaryTimer2");

		timers = processService.getProcessTimers(process.getId());

		assertTrue(timers.isEmpty());
	}
}
