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

package com.backstage.gradle

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

import static org.junit.jupiter.api.Assertions.*

class AgentDocsPluginTests
{
	@TempDir
	File projectDir

	@BeforeEach
	void init()
	{
		writeFile('settings.gradle', "rootProject.name = 'agent-docs-test'\n")
		writeFile('build.gradle', '''
			plugins {
				id 'base'
				id 'backstage-agent-docs'
			}

			repositories {
				maven {
					url = uri('repository')
				}
			}
		'''.stripIndent())

		def properties = new Properties()

		getClass().getResourceAsStream('/backstage.properties').withCloseable {
			properties.load(it)
		}

		publishDocs(properties.getProperty('backstageVersion'), documentationWithSkill('Default version'))
	}

	@Test
	void installsDocumentationWhenPluginIsApplied()
	{
		def result = runGradle('help')

		assertNotNull(result.task(':help'))
		assertNull(result.task(':syncAgentDocs'))
		assertEquals('Default version', readFile('.gradle/backstage/agent-docs/README.md'))
		assertTrue(readFile('AGENTS.md').contains('(<.gradle/backstage/agent-docs/README.md>)'))
		assertFalse(readFile('AGENTS.md').contains('syncAgentDocs'))
		assertFalse(new File(projectDir, '.gradle/backstage/agent-docs/META-INF').exists())
		assertTrue(new File(projectDir, '.agents/skills/backstage-dicts/SKILL.md').isFile())
		assertEquals('Default version', readFile('.agents/skills/backstage-dicts/references/capabilities.md'))
	}

	@Test
	void updatesSelectedVersionAndPreservesApplicationInstructions()
	{
		publishDocs('1.0', ['index.md': 'First version', 'old.md': 'Obsolete'])
		publishDocs('2.0', ['README.md': 'Second version', 'guides/audit.md': 'Audit guide'])
		writeFile('AGENTS.md', 'Application instructions')
		writeFile('CODEMAP.md', 'Application code map')

		runGradle('help', '-PbackstageVersion=1.0', '--configuration-cache')

		assertEquals('First version', readFile('.gradle/backstage/agent-docs/index.md'))
		assertTrue(readFile('AGENTS.md').contains('(<.gradle/backstage/agent-docs/index.md>)'))
		assertTrue(new File(projectDir, '.gradle/backstage/agent-docs/old.md').exists())

		runGradle('help', '-PbackstageVersion=2.0', '--configuration-cache')

		assertEquals('Second version', readFile('.gradle/backstage/agent-docs/README.md'))
		assertEquals('Audit guide', readFile('.gradle/backstage/agent-docs/guides/audit.md'))
		assertFalse(new File(projectDir, '.gradle/backstage/agent-docs/old.md').exists())
		assertFalse(new File(projectDir, '.gradle/backstage/agent-docs/index.md').exists())
		assertTrue(readFile('AGENTS.md').startsWith('Application instructions\n\n'))
		assertTrue(readFile('AGENTS.md').contains('(<.gradle/backstage/agent-docs/README.md>)'))
		assertEquals('Application code map', readFile('CODEMAP.md'))

		def instructions = readFile('AGENTS.md')

		runGradle('help', '-PbackstageVersion=2.0', '--configuration-cache')

		assertEquals(instructions, readFile('AGENTS.md'))
		assertEquals(1, instructions.count(AgentDocsPlugin.BLOCK_START))
		assertEquals(1, instructions.count(AgentDocsPlugin.BLOCK_END))
	}

	@Test
	void upgradesCustomDirectoryInstalledBeforeOwnershipMarker()
	{
		new File(projectDir, 'build.gradle').append('''
			agentDocs.outputDirectory = layout.projectDirectory.dir('.gradle/custom docs')
		'''.stripIndent(), 'UTF-8')
		writeFile('.gradle/custom docs/index.md', 'Legacy documentation')
		writeFile('AGENTS.md', "${AgentDocsPlugin.BLOCK_START}\n[Backstage](<.gradle/custom docs/index.md>)\n${AgentDocsPlugin.BLOCK_END}\n")

		runGradle('help')

		assertEquals('Default version', readFile('.gradle/custom docs/README.md'))
		assertFalse(new File(projectDir, '.gradle/custom docs/index.md').exists())
		assertTrue(readFile('AGENTS.md').contains('(<.gradle/custom docs/README.md>)'))
	}

	@ParameterizedTest
	@ValueSource(strings = ['.', 'docs'])
	void refusesToReplaceApplicationFiles(String outputDirectory)
	{
		new File(projectDir, 'build.gradle').append("\nagentDocs.outputDirectory = layout.projectDirectory.dir('$outputDirectory')\n", 'UTF-8')
		writeFile('docs/user.md', 'Application documentation')
		writeFile('AGENTS.md', 'Application instructions')

		def result = createGradleRunner('help').buildAndFail()

		assertTrue(result.output.contains('Backstage agent documentation'))
		assertEquals('Application documentation', readFile('docs/user.md'))
		assertEquals('Application instructions', readFile('AGENTS.md'))
		assertTrue(new File(projectDir, 'build.gradle').isFile())
	}

	@Test
	void preservesInstalledDocumentationWhenArchiveHasNoCatalog()
	{
		runGradle('help')
		def instructions = readFile('AGENTS.md')
		publishDocs('broken', ['guide.md': 'Incomplete documentation'])

		def result = createGradleRunner('help', '-PbackstageVersion=broken').buildAndFail()

		assertTrue(result.output.contains('must contain README.md or index.md'))
		assertEquals('Default version', readFile('.gradle/backstage/agent-docs/README.md'))
		assertEquals('Default version', readFile('.agents/skills/backstage-dicts/references/capabilities.md'))
		assertEquals(instructions, readFile('AGENTS.md'))
	}

	@Test
	void supportsExtensionOverridesAndRestoresFilesWithConfigurationCache()
	{
		publishDocs('3.0', documentationWithSkill('Extension version'))

		new File(projectDir, 'build.gradle').append('''
			agentDocs {
				version = '3.0'
				outputDirectory = layout.projectDirectory.dir('.gradle/custom-docs')
			}
		'''.stripIndent(), 'UTF-8')

		runGradle('help', '-PbackstageVersion=missing', '--configuration-cache')

		assertEquals('Extension version', readFile('.gradle/custom-docs/README.md'))
		assertTrue(readFile('AGENTS.md').contains('(<.gradle/custom-docs/README.md>)'))

		// После создания AGENTS.md Gradle повторно фиксирует его наличие в configuration cache.
		runGradle('help', '-PbackstageVersion=missing', '--configuration-cache')

		def repeated = runGradle('help', '-PbackstageVersion=missing', '--configuration-cache')

		assertTrue(repeated.output.contains('Reusing configuration cache.'), repeated.output)

		new File(projectDir, '.agents/skills/backstage-dicts').deleteDir()

		runGradle('help', '-PbackstageVersion=missing', '--configuration-cache')

		assertEquals('Extension version', readFile('.agents/skills/backstage-dicts/references/capabilities.md'))

		new File(projectDir, 'AGENTS.md').delete()
		new File(projectDir, '.gradle/custom-docs').deleteDir()

		runGradle('help', '-PbackstageVersion=missing', '--configuration-cache')

		assertEquals('Extension version', readFile('.gradle/custom-docs/README.md'))
		assertTrue(readFile('AGENTS.md').contains('(<.gradle/custom-docs/README.md>)'))
	}

	@Test
	void preservesSurroundingInstructionsAndReplacesOldTaskReference()
	{
		def prefix = '# Правила приложения\r\n\r\nСохраняй существующие инструкции.\r\n\r\n'
		def suffix = '\r\n\r\n## Проверки\r\nЗапускай тесты приложения.'
		def oldBlock = [
				AgentDocsPlugin.BLOCK_START,
				'Выполни ./gradlew syncAgentDocs',
				AgentDocsPlugin.BLOCK_END
		].join('\r\n')
		writeFile('AGENTS.md', prefix + oldBlock + suffix)

		new File(projectDir, 'build.gradle').append('''
			agentDocs {
				outputDirectory = layout.projectDirectory.dir('.gradle/backstage docs')
			}
		'''.stripIndent(), 'UTF-8')

		runGradle('help')

		def updated = readFile('AGENTS.md')

		assertTrue(updated.startsWith(prefix))
		assertTrue(updated.endsWith(suffix))
		assertTrue(updated.contains('(<.gradle/backstage docs/README.md>)'))
		assertFalse(updated.contains('syncAgentDocs'))
		assertFalse(updated.replace('\r\n', '').contains('\n'))
	}

	@ParameterizedTest
	@ValueSource(strings = [
			'<!-- backstage-agent-docs:start -->\nUnclosed instructions',
			'Instructions without a start marker\n<!-- backstage-agent-docs:end -->',
			'<!-- backstage-agent-docs:end -->\n<!-- backstage-agent-docs:start -->',
			'<!-- backstage-agent-docs:start --><!-- backstage-agent-docs:end --><!-- backstage-agent-docs:start --><!-- backstage-agent-docs:end -->'
	])
	void preservesInstructionsWhenMarkersAreMalformed(String content)
	{
		writeFile('AGENTS.md', content)

		def result = createGradleRunner('help').buildAndFail()

		assertTrue(result.output.contains('expected a single complete Backstage documentation block'))
		assertEquals(content, readFile('AGENTS.md'))
		assertFalse(new File(projectDir, '.gradle/backstage/agent-docs').exists())
		assertFalse(new File(projectDir, '.agents/skills/backstage-dicts').exists())
	}

	@Test
	void installsDocumentationAtRootWhenAppliedToSubproject()
	{
		writeFile('settings.gradle', "rootProject.name = 'agent-docs-test'\ninclude 'application', 'local-library'\n")
		writeFile('build.gradle', "plugins { id 'backstage-agent-docs' apply false }\n")
		writeFile('local-library/build.gradle', "plugins { id 'java-library' }\n")
		writeFile('AGENTS.md', "Application instructions\n\n${AgentDocsPlugin.BLOCK_START}\n[Backstage](<application/.gradle/backstage/agent-docs/README.md>)\n${AgentDocsPlugin.BLOCK_END}\n")
		publishArtifact('example', 'bot-sdk', '1.0', [
				'META-INF/backstage/agents/skills/bot-sdk/SKILL.md': 'SDK skill'
		])
		writeFile('application/build.gradle', '''
			plugins {
				id 'java'
				id 'backstage-agent-docs'
			}

			dependencies {
				implementation project(':local-library')
				implementation 'example:bot-sdk:1.0'
			}

			repositories {
				maven {
					url = rootProject.uri('repository')
				}
			}
		'''.stripIndent())

		def result = runGradle('help')
		def instructions = readFile('AGENTS.md')

		assertNull(result.task(':local-library:jar'))
		assertTrue(instructions.startsWith('Application instructions\n\n'))
		assertTrue(instructions.contains('(<.gradle/backstage/agent-docs/LIBRARIES.md>)'))
		assertFalse(instructions.contains('application/.gradle/backstage/agent-docs/'))
		assertEquals('Default version', readFile('.gradle/backstage/agent-docs/README.md'))
		assertEquals('SDK skill', readFile('.gradle/backstage/agent-docs/example/bot-sdk/skills/bot-sdk/SKILL.md'))
		assertEquals('SDK skill', readFile('.agents/skills/bot-sdk/SKILL.md'))
		assertFalse(new File(projectDir, 'application/.gradle/backstage/agent-docs').exists())
		assertFalse(new File(projectDir, 'application/AGENTS.md').exists())
		assertTrue(new File(projectDir, '.agents/skills/backstage-dicts/SKILL.md').isFile())
		assertFalse(new File(projectDir, 'application/.agents/skills/backstage-dicts').exists())
	}

	@Test
	void updatesAndRemovesOnlyManagedSkills()
	{
		def first = documentationWithSkill('First version')
		first.put('skills/backstage-dicts/references/obsolete.md', 'Obsolete')
		publishDocs('1.0', first)
		publishDocs('2.0', documentationWithSkill('Second version'))
		publishDocs('3.0', ['README.md': 'No skills'])
		writeFile('.agents/skills/custom/SKILL.md', 'Application skill')

		runGradle('help', '-PbackstageVersion=1.0')

		assertTrue(new File(projectDir, '.agents/skills/backstage-dicts/references/obsolete.md').isFile())

		runGradle('help', '-PbackstageVersion=2.0')

		assertEquals('Second version', readFile('.agents/skills/backstage-dicts/references/capabilities.md'))
		assertFalse(new File(projectDir, '.agents/skills/backstage-dicts/references/obsolete.md').exists())
		assertEquals('Application skill', readFile('.agents/skills/custom/SKILL.md'))

		runGradle('help', '-PbackstageVersion=3.0')

		assertFalse(new File(projectDir, '.agents/skills/backstage-dicts').exists())
		assertEquals('Application skill', readFile('.agents/skills/custom/SKILL.md'))
	}

	@Test
	void preservesExistingSkillWhenNameConflicts()
	{
		writeFile('.agents/skills/backstage-dicts/SKILL.md', 'Application skill')
		writeFile('AGENTS.md', 'Application instructions')

		def result = createGradleRunner('help').buildAndFail()

		assertTrue(result.output.contains('already exists and is not managed by backstage-agent-docs'))
		assertEquals('Application skill', readFile('.agents/skills/backstage-dicts/SKILL.md'))
		assertEquals('Application instructions', readFile('AGENTS.md'))
		assertFalse(new File(projectDir, '.gradle/backstage/agent-docs').exists())
	}

	@Test
	void keepsDocumentationAndInstructionsAfterCleanBuild()
	{
		writeFile('AGENTS.md', '# Правила приложения\n')

		runGradle('help')

		def instructions = readFile('AGENTS.md')

		runGradle('clean', 'build')

		assertEquals(instructions, readFile('AGENTS.md'))
		assertEquals('Default version', readFile('.gradle/backstage/agent-docs/README.md'))
	}

	@Test
	void supportsPrebuiltDocumentationDependency()
	{
		publishDocs('local', ['README.md': 'Local documentation'])

		new File(projectDir, 'build.gradle').append('''
			dependencies {
				agentDocs files('repository/com/backstage/agents/local/agents-local.jar')
			}
		'''.stripIndent(), 'UTF-8')

		runGradle('help', '-PbackstageVersion=missing')

		assertEquals('Local documentation', readFile('.gradle/backstage/agent-docs/README.md'))
	}

	@Test
	void installsAndUpdatesDocumentationFromResolvedRuntimeJars()
	{
		def prefix = 'META-INF/backstage/agents/'
		publishArtifact('example', 'bot-sdk', '1.0', [
				("${prefix}README.md"): 'SDK guide',
				("${prefix}skills/bot-sdk/SKILL.md"): 'SDK 1.0 skill',
				("${prefix}skills/bot-sdk/assets/example.json"): '{"version": 1}',
				'skills/ignored/SKILL.md': 'Outside the documentation directory'
		])
		publishArtifact('example', 'bot-sdk', '2.0', [
				("${prefix}skills/bot-sdk/SKILL.md"): 'SDK 2.0 skill'
		])
		publishArtifact('example', 'bot-client', '1.0', [:], ['example:bot-sdk:1.0'])
		publishArtifact('example', 'bot-client', '2.0', [:], ['example:bot-sdk:2.0'])
		writeFile('.agents/skills/local/SKILL.md', 'Application skill')
		writeFile('AGENTS.md', 'Application instructions\n')

		new File(projectDir, 'build.gradle').append('''
			apply plugin: 'java'

			dependencies {
				if (providers.gradleProperty('clientVersion').isPresent()) {
					implementation "example:bot-client:${providers.gradleProperty('clientVersion').get()}"
				}
				testImplementation 'missing:test-only:1.0'
			}
		'''.stripIndent(), 'UTF-8')

		runGradle('help', '-PclientVersion=1.0', '--configuration-cache')

		assertEquals('SDK 1.0 skill', readFile('.agents/skills/bot-sdk/SKILL.md'))
		assertEquals('{"version": 1}', readFile('.agents/skills/bot-sdk/assets/example.json'))
		assertEquals('SDK guide', readFile('.gradle/backstage/agent-docs/example/bot-sdk/README.md'))
		assertTrue(readFile('.gradle/backstage/agent-docs/LIBRARIES.md').contains('example/bot-sdk/README.md'))
		assertFalse(new File(projectDir, '.agents/skills/ignored').exists())
		assertTrue(readFile('AGENTS.md').startsWith('Application instructions\n'))
		assertTrue(readFile('AGENTS.md').contains('(<.gradle/backstage/agent-docs/LIBRARIES.md>)'))

		runGradle('help', '-PclientVersion=1.0', '--configuration-cache')
		def repeated = runGradle('help', '-PclientVersion=1.0', '--configuration-cache')
		assertTrue(repeated.output.contains('Reusing configuration cache.'), repeated.output)

		new File(projectDir, '.agents/skills/bot-sdk/assets/example.json').delete()
		runGradle('help', '-PclientVersion=1.0', '--configuration-cache')
		assertEquals('{"version": 1}', readFile('.agents/skills/bot-sdk/assets/example.json'))

		runGradle('help', '-PclientVersion=2.0', '--configuration-cache')

		assertEquals('SDK 2.0 skill', readFile('.agents/skills/bot-sdk/SKILL.md'))
		assertFalse(new File(projectDir, '.agents/skills/bot-sdk/assets/example.json').exists())
		assertTrue(readFile('.gradle/backstage/agent-docs/LIBRARIES.md').contains('example/bot-sdk/skills/bot-sdk/SKILL.md'))
		assertFalse(new File(projectDir, '.gradle/backstage/agent-docs/example/bot-sdk/README.md').exists())

		runGradle('help', '--configuration-cache')

		assertFalse(new File(projectDir, '.agents/skills/bot-sdk').exists())
		assertFalse(new File(projectDir, '.gradle/backstage/agent-docs/example').exists())
		assertFalse(new File(projectDir, '.gradle/backstage/agent-docs/LIBRARIES.md').exists())
		assertTrue(readFile('AGENTS.md').contains('(<.gradle/backstage/agent-docs/README.md>)'))
		assertEquals('Application skill', readFile('.agents/skills/local/SKILL.md'))
		assertEquals('Default version', readFile('.agents/skills/backstage-dicts/references/capabilities.md'))
	}

	@Test
	void rejectsDuplicateSkillsBeforeUpdatingInstalledDocumentation()
	{
		runGradle('help')
		def instructions = readFile('AGENTS.md')
		publishArtifact('example', 'bot-sdk', '1.0', [
				'META-INF/backstage/agents/skills/backstage-dicts/SKILL.md': 'Conflicting SDK skill'
		])
		new File(projectDir, 'build.gradle').append('''
			apply plugin: 'java'

			dependencies {
				implementation 'example:bot-sdk:1.0'
			}
		'''.stripIndent(), 'UTF-8')

		def result = createGradleRunner('help').buildAndFail()

		assertTrue(result.output.contains("Cannot install Backstage skill 'backstage-dicts'"))
		assertTrue(result.output.contains('agents-'))
		assertTrue(result.output.contains('example:bot-sdk:1.0'))
		assertEquals(instructions, readFile('AGENTS.md'))
		assertEquals('Default version', readFile('.agents/skills/backstage-dicts/references/capabilities.md'))
		assertFalse(new File(projectDir, '.gradle/backstage/agent-docs/example').exists())
	}

	private BuildResult runGradle(String... arguments)
	{
		return createGradleRunner(arguments).build()
	}

	private Map<String, String> documentationWithSkill(String content)
	{
		return [
				'README.md': content,
				'skills/backstage-dicts/SKILL.md': '---\nname: backstage-dicts\ndescription: Backstage dictionaries\n---\n\nRead references/capabilities.md.\n',
				'skills/backstage-dicts/references/capabilities.md': content
		]
	}

	private GradleRunner createGradleRunner(String... arguments)
	{
		return GradleRunner.create()
				.withProjectDir(projectDir)
				.withPluginClasspath()
				.withArguments(arguments.toList() + ['--offline', '--stacktrace'])
	}

	private void publishDocs(String version, Map<String, String> documents)
	{
		publishArtifact('com.backstage', 'agents', version, documents, ['missing:runtime-dependency:1.0'])
	}

	private void publishArtifact(String group, String artifact, String version, Map<String, String> documents, List<String> dependencies = [])
	{
		def directory = new File(projectDir, "repository/${group.replace('.', '/')}/$artifact/$version")
		directory.mkdirs()

		def pomDependencies = dependencies.collect { dependency ->
			def (dependencyGroup, dependencyArtifact, dependencyVersion) = dependency.split(':')
			return "<dependency><groupId>$dependencyGroup</groupId><artifactId>$dependencyArtifact</artifactId><version>$dependencyVersion</version></dependency>"
		}.join('\n')

		new File(directory, "$artifact-${version}.pom").write("""
			<project>
				<modelVersion>4.0.0</modelVersion>
				<groupId>$group</groupId>
				<artifactId>$artifact</artifactId>
				<version>$version</version>
				<dependencies>
					$pomDependencies
				</dependencies>
			</project>
		""".stripIndent(), 'UTF-8')

		new JarOutputStream(new FileOutputStream(new File(directory, "$artifact-${version}.jar"))).withCloseable { jar ->
			def entries = ['META-INF/MANIFEST.MF': 'Manifest-Version: 1.0\n'] + documents

			entries.each { name, content ->
				jar.putNextEntry(new JarEntry(name))
				jar.write(content.getBytes('UTF-8'))
				jar.closeEntry()
			}
		}
	}

	private void writeFile(String path, String content)
	{
		def file = new File(projectDir, path)
		file.parentFile.mkdirs()
		file.write(content, 'UTF-8')
	}

	private String readFile(String path)
	{
		return new File(projectDir, path).getText('UTF-8')
	}
}
