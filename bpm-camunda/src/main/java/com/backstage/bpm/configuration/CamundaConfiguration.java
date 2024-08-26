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

package com.backstage.bpm.configuration;

import com.backstage.bpm.configuration.properties.BpmProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class CamundaConfiguration
{
	private final BpmProperties bpmProperties;

	@PostConstruct
	public void initialize()
	{
		// TODO: implement
		if (bpmProperties.getOverriddenTimerDelay() > 0)
		{
			log.warn("Timer delay override is not supported for Camunda BPM!");
		}

		// TODO: implement
		if (bpmProperties.isPessimisticLocking())
		{
			log.warn("Pessimistic locking is not supported for Camunda BPM!");
		}
	}
}
