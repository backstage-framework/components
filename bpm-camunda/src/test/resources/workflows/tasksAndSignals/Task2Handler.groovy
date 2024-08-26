/*
 *    Copyright 2019-2023 the original author or authors.
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

package workflows.tasksAndSignals

import com.backstage.bpm.domain.Task
import com.backstage.bpm.service.handler.AbstractTaskHandler
import jakarta.annotation.Nonnull

class Task2Handler extends AbstractTaskHandler
{
	@Override
	void onTaskAbort(@Nonnull Task task)
	{
		task.process.parameters.put("task2Canceled", true)
	}
}
