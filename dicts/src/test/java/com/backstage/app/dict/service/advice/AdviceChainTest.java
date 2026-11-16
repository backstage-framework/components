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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AdviceChainTest
{
	private final DictDataServiceAdvice regular = new DictDataServiceAdvice() { };

	private final DictDataServiceAdvice migrationSafe = new DictDataServiceAdvice() {
		@Override
		public boolean applicableDuringMigration()
		{
			return true;
		}
	};

	private final AdviceChain<DictDataServiceAdvice> chain = new AdviceChain<>(List.of(regular, migrationSafe),
			DictDataServiceAdvice::applicableDuringMigration);

	@Test
	void allAdvicesAreInvokedWithoutSuppression()
	{
		assertEquals(List.of(regular, migrationSafe), collect());
	}

	@Test
	void onlyMigrationSafeAdvicesAreInvokedWhenSuppressed()
	{
		var invoked = DictAdviceSuppression.callWithoutAdvice(this::collect);

		assertEquals(List.of(migrationSafe), invoked);
	}

	@Test
	void suppressionIsReentrantAndResetAfterwards()
	{
		DictAdviceSuppression.runWithoutAdvice(() -> {
			DictAdviceSuppression.runWithoutAdvice(() -> assertTrue(DictAdviceSuppression.isSuppressed()));

			assertTrue(DictAdviceSuppression.isSuppressed());
		});

		assertFalse(DictAdviceSuppression.isSuppressed());
		assertEquals(List.of(regular, migrationSafe), collect());
	}

	@Test
	void suppressionIsResetAfterException()
	{
		assertThrows(IllegalStateException.class, () -> DictAdviceSuppression.runWithoutAdvice(() -> {
			throw new IllegalStateException();
		}));

		assertFalse(DictAdviceSuppression.isSuppressed());
	}

	@Test
	void suppressionIsNotVisibleInOtherThreads()
	{
		var otherThreadSuppressed = new ArrayList<Boolean>();

		DictAdviceSuppression.runWithoutAdvice(() -> {
			var thread = new Thread(() -> otherThreadSuppressed.add(DictAdviceSuppression.isSuppressed()));

			thread.start();

			try
			{
				thread.join();
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
			}
		});

		assertEquals(List.of(false), otherThreadSuppressed);
	}

	private List<DictDataServiceAdvice> collect()
	{
		var result = new ArrayList<DictDataServiceAdvice>();

		chain.forEach(result::add);

		return result;
	}
}
