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

package com.backstage.bpm.model.event;

import org.springframework.context.ApplicationEvent;

/**
 * Событие отправляется после того, как список доступных процессов был изменён. Например, после деплоя
 * нового процесса или первоначальной инициализации процессов.
 */
public class WorkflowsUpdatedEvent extends ApplicationEvent
{
	public WorkflowsUpdatedEvent(Object source)
	{
		super(source);
	}
}
