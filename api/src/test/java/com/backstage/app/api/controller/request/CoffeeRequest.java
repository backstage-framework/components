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

package com.backstage.app.api.controller.request;

import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class CoffeeRequest
{
	private CupSize size;

	private List<CoffeeType> types;

	private Map<Additive, Integer> additives;

	private Wish wish;

	private List<Wish> wishes;

	public enum CoffeeType
	{
		LATTE, CAPPUCCINO, AMERICANO, ESPRESSO, FLAT_WHITE
	}

	public enum CupSize
	{
		SMALL, MEDIUM, BIG
	}

	public enum Additive
	{
		SUGAR, MILK, SYRUP
	}

	@Getter
	public static class Wish
	{
		public MilkTemperature temperature;

		public List<MilkTemperature> temperatures;

		public Map<MilkTemperature, Integer> temperatureCounts;

		public enum MilkTemperature
		{
			HOT, COLD, WARM
		}
	}
}
