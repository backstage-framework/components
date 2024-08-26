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

package com.backstage.bpm.service.handler;

import com.backstage.bpm.domain.Task;
import com.backstage.bpm.domain.TaskAction;
import jakarta.annotation.Nonnull;

/**
 * Интерфейс обработчика задач бизнес-процесса. Позволяет реагировать на все этапы жизненного цикла
 * задачи @{@link Task}.
 */
public abstract class AbstractTaskHandler
{
	/**
	 * Вызывается во время назначения задачи на конкретного пользователя. Назначаемый пользователь будет
	 * указан в @{@link Task#getUserId()}.
	 */
	public void onTaskAssign(@Nonnull Task task)
	{
	}

	/**
	 * Вызывается во время создания новой задачи.
	 */
	public void onTaskCreate(@Nonnull Task task)
	{
	}

	/**
	 * Вызывается во время завершения задачи. В момент вызова в поле @{@link Task#getResult()}
	 * находится актуальное значение.
	 */
	public void onTaskComplete(@Nonnull Task task)
	{
	}

	/**
	 * Вызывается во время снятия ранее созданной задачи.
	 */
	public void onTaskAbort(@Nonnull Task task)
	{
	}

	protected void addAction(Task task, String actionId, String actionName)
	{
		task.getActions().add(new TaskAction(actionId, actionName));
	}
}
