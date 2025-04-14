/*
 *    Copyright 2019-2025 the original author or authors.
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

package com.backstage.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.Property

class DictsCodegenPlugin implements Plugin<Project>
{
	final BACKSTAGE_PACKAGE = "com.backstage"
	final BACKSTAGE_CODEGEN_ARTIFACT = "dicts-codegen"
	final BACKSTAGE_VERSION_PROPERTY = "backstageVersion"

	@Override
	void apply(Project project)
	{
		def extension = project.extensions.create('dictsCodegen', DictsCodegenPluginExtension)

		project.afterEvaluate {
			def targetProject = extension.targetProject.get()
			def backstageVersion

			if (!extension.version.present)
			{
				if (project.hasProperty(BACKSTAGE_VERSION_PROPERTY))
				{
					backstageVersion = project.property(BACKSTAGE_VERSION_PROPERTY)
				}
				else
				{
					backstageVersion = getDefaultBackstageVersion()

					println("No Backstage version specified for dicts codegen plugin, using default '$backstageVersion'.")
				}
			}
			else
			{
				backstageVersion = extension.version.get()
			}

			def configurationName = targetProject.configurations.names.contains("api") ? "api" : "implementation"

			targetProject.configurations.named(configurationName).get().dependencies.add(
					targetProject.dependencies.create("$BACKSTAGE_PACKAGE:$BACKSTAGE_CODEGEN_ARTIFACT:$backstageVersion"))

			var bootRunProvider = project.tasks.named('bootRun')

			if (!bootRunProvider.present)
			{
				println("Dicts codegen plugin requires Spring Boot (bootRun task).")

				return
			}

			var bootRun = bootRunProvider.get()
			var outputPath = targetProject.sourceSets.main.java.srcDirs.first().toString()

			project.tasks.register("dictsCodegen", bootRun.class as Class<Task>, {
				group = 'backstage'
				classpath = bootRun.classpath
				mainClass = bootRun.mainClass

				environment("app.dicts.codegen.outputPath", outputPath)
				environment("app.dicts.codegen.targetPackage", extension.targetPackage.get())
			})
		}
	}

	private String getDefaultBackstageVersion()
	{
		def properties = getClass().getResourceAsStream("/backstage.properties").with {
			def properties = new Properties();
			properties.load(it)

			return properties
		}

		return properties.get(BACKSTAGE_VERSION_PROPERTY)
	}
}

class DictsCodegenPluginExtension
{
	Property<Project> targetProject

	Property<String> targetPackage

	Property<String> version

	DictsCodegenPluginExtension(Project project)
	{
		targetProject = project.objects.property(Project.class).convention(project)
		targetPackage = project.objects.property(String.class).convention("com.backstage.dicts.generated")
		version = project.objects.property(String.class)
	}
}
