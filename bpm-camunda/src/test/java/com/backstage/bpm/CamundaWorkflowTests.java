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

import com.backstage.bpm.model.ExportedWorkflow;
import com.backstage.bpm.service.process.ProcessService;
import com.backstage.bpm.service.workflow.ClasspathWorkflowProvider;
import com.backstage.bpm.service.workflow.WorkflowService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Cleanup;
import lombok.SneakyThrows;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CamundaWorkflowTests extends AbstractTests
{
	private final String WORKFLOW_NAME = "migration";
	private final String WORKFLOW_FIRST_REVISION = WORKFLOW_NAME + "_1.0";
	private final String WORKFLOW_SECOND_REVISION = WORKFLOW_NAME + "_2.0";

	@Autowired private ObjectMapper objectMapper;

	@Autowired private WorkflowService workflowService;
	@Autowired private ProcessService processService;
	@Autowired private ClasspathWorkflowProvider classpathWorkflowProvider;

	@Value("classpath:exportedWorkflows/migration_1.0.json")
	private Resource workflow1;

	@Value("classpath:exportedWorkflows/migration_2.0.json")
	private Resource workflow2;

	@Test
	public void exportWorkflows()
	{
		var exportedWorkflows = classpathWorkflowProvider.exportWorkflows();

		assertFalse(exportedWorkflows.isEmpty());
	}

	@Order(1)
	@Test
	@SneakyThrows
	public void deployRevisions()
	{
		@Cleanup var workflowStream1 = workflow1.getInputStream();
		@Cleanup var workflowStream2 = workflow2.getInputStream();

		ExportedWorkflow workflow1 = objectMapper.readerFor(ExportedWorkflow.class).readValue(workflowStream1);
		ExportedWorkflow workflow2 = objectMapper.readerFor(ExportedWorkflow.class).readValue(workflowStream2);

		workflowService.deployWorkflow(workflow1.getId(), null, workflow1.getDefinition(), false);
		workflowService.checkWorkflows();

		workflowService.deployWorkflow(workflow2.getId(), null, workflow2.getDefinition(), false);
		workflowService.checkWorkflows();

		assertEquals(workflowService.getActualWorkflowId(WORKFLOW_NAME), WORKFLOW_SECOND_REVISION);
	}

	@Order(2)
	@Test
	public void migrateRevisions()
	{
		var process = processService.startProcess(WORKFLOW_FIRST_REVISION);

		processService.migrateProcess(process.getId(), WORKFLOW_SECOND_REVISION);

		process = processService.getProcess(process.getId()).orElse(null);

		assertNotNull(process);
		assertEquals(process.getWorkflowId(), WORKFLOW_SECOND_REVISION);

		processService.sendEvent(process, "nextStep1");

		process = processService.getProcess(process.getId()).orElse(null);

		assertNotNull(process);
		assertTrue(process.isActive());

		processService.sendEvent(process, "nextStep2");

		process = processService.getProcess(process.getId()).orElse(null);

		assertNotNull(process);
		assertFalse(process.isActive());
	}

	@Order(3)
	@Test
	void initializationTest()
	{
		workflowService.initialize();
	}
}
