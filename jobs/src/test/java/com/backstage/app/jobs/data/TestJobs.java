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

package com.backstage.app.jobs.data;

import com.backstage.app.jobs.dto.other.JobResult;
import com.backstage.app.jobs.dto.param.EmptyJobParams;
import com.backstage.app.jobs.dto.param.JobParams;
import com.backstage.app.jobs.service.AbstractCronJob;
import com.backstage.app.jobs.service.AbstractFixedDelayJob;
import com.backstage.app.jobs.service.AbstractManualJob;
import com.backstage.app.jobs.service.JobDescription;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class TestJobs
{
	public static final String TEST_CRON = "0 0 4 * * *";

	public static final String TEST_CRON_RESCHEDULE = "0 0 1 2 1 *";

	public static final String TEST_UPDATED_CRON = "0 0 0 1 1 *";
	public static final String TEST_DESCRIPTION = "test";

	public static final long TEST_FIXED_DELAY = 10000;

	@Component
	public static class TestJobWithoutDescription extends AbstractCronJob<EmptyJobParams>
	{
		@Override
		public String getCronExpression()
		{
			return TEST_CRON;
		}

		@Override
		protected JobResult execute()
		{
			return JobResult.ok();
		}
	}

	@Component
	@JobDescription
	public static class TestJobWithEmptyDescription extends AbstractCronJob<EmptyJobParams>
	{
		@Override
		public String getCronExpression()
		{
			return TEST_CRON;
		}

		@Override
		protected JobResult execute()
		{
			return JobResult.ok();
		}
	}

	@Component
	@JobDescription(TEST_DESCRIPTION)
	public static class TestJobWithDescription extends AbstractCronJob<EmptyJobParams>
	{
		@Override
		public String getCronExpression()
		{
			return TEST_CRON;
		}

		@Override
		protected JobResult execute()
		{
			return JobResult.ok();
		}
	}

	@Component
	public static class TestCronJobRescheduling extends AbstractCronJob<EmptyJobParams>
	{
		@Override
		public String getCronExpression()
		{
			return TEST_CRON_RESCHEDULE;
		}

		@Override
		protected JobResult execute()
		{
			return JobResult.ok();
		}
	}

	@Component
	public static class TestManualJobRescheduling extends AbstractManualJob<EmptyJobParams>
	{
		@Override
		protected JobResult execute()
		{
			return JobResult.ok();
		}
	}

	@Component
	public static class TestFixedDelayRescheduling extends AbstractFixedDelayJob<EmptyJobParams>
	{
		@Override
		protected JobResult execute()
		{
			return JobResult.ok();
		}

		@Override
		public long getFixedDelay()
		{
			return TEST_FIXED_DELAY;
		}
	}

	@Component
	public static class TestManualJobWithParams extends AbstractManualJob<TestManualJobParams>
	{
		@Override
		protected JobResult execute(TestManualJobParams params)
		{
			var resultParams = new HashMap<String, Object>(1);
			resultParams.put("result", params.testParam);

			return JobResult.builder()
					.properties(resultParams)
					.build();
		}

		@Override
		protected TestManualJobParams createDefaultParams()
		{
			return new TestManualJobParams("defaultValue");
		}
	}

	@Component
	public static class TestManualJobWithValidationParams extends AbstractManualJob<TestValidationJobParams>
	{
		@Override
		protected JobResult execute(TestValidationJobParams params)
		{
			return JobResult.builder()
					.properties(Map.of("result", params.testInteger))
					.build();
		}

		@Override
		protected TestValidationJobParams createDefaultParams()
		{
			return new TestValidationJobParams(0, LocalDate.now());
		}
	}

	@Getter
	@NoArgsConstructor
	@EqualsAndHashCode
	@AllArgsConstructor
	public static class TestManualJobParams implements JobParams
	{
		private String testParam;
	}

	@Getter
	@NoArgsConstructor
	@EqualsAndHashCode
	@AllArgsConstructor
	public static class TestValidationJobParams implements JobParams
	{
		@Positive
		private Integer testInteger;

		@NotNull
		private LocalDate testDate;
	}
}
