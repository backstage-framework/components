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

import org.jspecify.annotations.NonNull;

import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

/**
 * Упорядоченная цепочка advice'ов, учитывающая {@link DictAdviceSuppression}.
 * <p>
 * Пока подавление активно, итерация возвращает только advice'ы, применимые во время миграции.
 * Благодаря этому места вызова ({@code forEach}, {@code for}) не требуют дополнительных проверок.
 * <p>
 * Применимость advice к миграциям определяется один раз при создании цепочки.
 */
public class AdviceChain<T> implements Iterable<T>
{
	private final List<T> advices;
	private final List<T> migrationApplicableAdvices;

	public AdviceChain(List<T> advices, Predicate<T> applicableDuringMigration)
	{
		this.advices = List.copyOf(advices);
		this.migrationApplicableAdvices = advices.stream()
				.filter(applicableDuringMigration)
				.toList();
	}

	@Override
	public @NonNull Iterator<T> iterator()
	{
		return (DictAdviceSuppression.isSuppressed() ? migrationApplicableAdvices : advices).iterator();
	}

	/**
	 * @return все зарегистрированные advice'ы без учета подавления.
	 */
	public List<T> getAll()
	{
		return advices;
	}
}
