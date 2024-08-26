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

package com.backstage.bpm.service;

import com.backstage.bpm.exception.BpmException;
import com.backstage.bpm.model.EngineType;
import com.backstage.bpm.model.Workflow;
import com.backstage.bpm.model.event.WorkflowsUpdatedEvent;
import com.backstage.bpm.service.workflow.WorkflowService;
import com.backstage.bpm.utils.DOMUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.repository.Deployment;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CamundaKnowledgeService implements ApplicationListener<WorkflowsUpdatedEvent>
{
	private final ProcessEngine processEngine;

	private final WorkflowService workflowService;

	public void onApplicationEvent(WorkflowsUpdatedEvent event)
	{
		initialize();
	}

	@PostConstruct
	public synchronized void initialize()
	{
		try
		{
			readKnowledgeBase();
		}
		catch (Exception e)
		{
			log.error("Failed to load workflow definitions.", e);
		}
	}

	private void readKnowledgeBase()
	{
		log.info("Initializing camunda knowledge base.");

		for (var workflow : workflowService.getWorkflows().stream().filter(it -> it.getEngineType() == EngineType.CAMUNDA).toList())
		{
			var deploymentName = workflow.getFullId();
			var versions = processEngine.getRepositoryService().createProcessDefinitionQuery().processDefinitionKey(workflow.getId()).orderByProcessDefinitionVersion().desc().list();

			boolean alreadyDeployed = versions.stream()
					.map(version -> processEngine.getRepositoryService().createDeploymentQuery().deploymentId(version.getDeploymentId()).singleResult())
					.map(Deployment::getName)
					.anyMatch(deploymentName::equals);

			if (alreadyDeployed)
			{
				continue;
			}

			var deployment = processEngine.getRepositoryService().createDeployment();

			for (var script : workflow.getScripts().values())
			{
				deployment.addString(script.getFilename(), script.getDefinition());
			}

			deployment.addString(workflow.getId() + ".bpmn", processWorkflow(workflow));
			deployment.name(deploymentName);
			deployment.deploy();
		}
	}

	private String processWorkflow(Workflow workflow)
	{
		try
		{
			var document = DOMUtils.parseDocument(workflow.getDefinition());
			var rootNamespace = DOMUtils.getRootNamespace(workflow);

			Element processNode = (Element) document.getDocumentElement().getElementsByTagName(rootNamespace + ":process").item(0);

			if (processNode == null)
			{
				processNode = (Element) document.getDocumentElement().getElementsByTagName("process").item(0);

				if (processNode == null)
				{
					throw new BpmException(String.format("cannot find process node in workflow '%s'", workflow.getId()));
				}
			}

			removeExtensionElements(rootNamespace, processNode);

			validateScriptTasks(processNode.getElementsByTagName(rootNamespace + ":scriptTask"));

			processNode.setAttribute("id", workflow.getId());
			processNode.setAttribute("camunda:versionTag", workflow.getVersion().toString());

			return DOMUtils.writeDocument(document);
		}
		catch (Exception e)
		{
			throw new BpmException("failed to patch workflow '%s' definition".formatted(workflow.getFullId()), e);
		}
	}

	private void removeExtensionElements(String rootNamespace, Element processNode)
	{
		NodeList processScripts;

		while ((processScripts = processNode.getElementsByTagName("processScript")).getLength() > 0)
		{
			var item = processScripts.item(0);

			item.getParentNode().removeChild(item);
		}

		List<Element> emptyExtensionElements = new ArrayList<>();
		var extensionElements = (NodeList) processNode.getElementsByTagName(rootNamespace + ":extensionElements");

		for (int i = 0; i < extensionElements.getLength(); i++)
		{
			var item = (Element) extensionElements.item(i);

			if (item.getChildNodes().getLength() == 0)
			{
				emptyExtensionElements.add(item);
			}
		}

		emptyExtensionElements.forEach(element -> element.getParentNode().removeChild(element));
	}

	private void validateScriptTasks(NodeList scriptTasks)
	{
		for (int i = 0; i < scriptTasks.getLength(); i++)
		{
			var scriptTask = (Element) scriptTasks.item(i);

			if (!scriptTask.hasAttribute("scriptFormat"))
			{
				throw new BpmException("script node '%s' has no scripting language specified".formatted(scriptTask.getAttribute("id")));
			}
		}
	}
}
