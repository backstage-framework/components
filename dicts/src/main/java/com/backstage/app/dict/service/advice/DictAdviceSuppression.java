/*
 *    Copyright 2019-2026 the original author or authors.
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

package com.backstage.app.dict.service.advice;

import lombok.experimental.UtilityClass;

import java.util.function.Supplier;

/**
 * Контекст подавления advice'ов {@link DictServiceAdvice} и {@link DictDataServiceAdvice} в текущем потоке.
 * <p>
 * Используется при выполнении миграций: пока контекст активен, {@link AdviceChain} пропускает все advice'ы,
 * кроме тех, у которых {@link DictServiceAdvice#applicableDuringMigration()} /
 * {@link DictDataServiceAdvice#applicableDuringMigration()} возвращает {@code true}.
 * <p>
 * Контекст привязан к потоку и поддерживает вложенность. Выполнение должно быть синхронным в рамках одного потока:
 * при передаче работы в другой поток подавление не распространяется.
 */
@UtilityClass
public class DictAdviceSuppression
{
	private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

	public boolean isSuppressed()
	{
		return DEPTH.get() > 0;
	}

	public void runWithoutAdvice(Runnable action)
	{
		callWithoutAdvice(() -> {
			action.run();

			return null;
		});
	}

	public <T> T callWithoutAdvice(Supplier<T> action)
	{
		DEPTH.set(DEPTH.get() + 1);

		try
		{
			return action.get();
		}
		finally
		{
			var depth = DEPTH.get() - 1;

			if (depth == 0)
			{
				DEPTH.remove();
			}
			else
			{
				DEPTH.set(depth);
			}
		}
	}
}
