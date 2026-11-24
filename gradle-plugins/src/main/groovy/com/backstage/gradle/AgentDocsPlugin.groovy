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

import groovy.transform.TupleConstructor
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Property

import javax.inject.Inject
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path

abstract class AgentDocsPlugin implements Plugin<Project>
{
	static final String BLOCK_START = '<!-- backstage-agent-docs:start -->'
	static final String BLOCK_END = '<!-- backstage-agent-docs:end -->'
	private static final String MANAGED_MARKER = '.backstage-agent-docs'
	private static final String RESOURCE_PREFIX = 'META-INF/backstage/agents/'
	private static final String LIBRARY_CATALOG = 'LIBRARIES.md'
	private static final String LEGACY_OUTPUT_DIRECTORY = '.gradle/backstage/agent-docs'
	private static final List<String> CATALOG_NAMES = ['README.md', 'index.md']

	@Inject
	abstract ArchiveOperations getArchiveOperations()

	@Inject
	abstract FileSystemOperations getFileSystemOperations()

	@Override
	void apply(Project project)
	{
		def extension = project.extensions.create('agentDocs', AgentDocsPluginExtension)
		def defaultDirectory = project.rootProject.layout.projectDirectory.dir('.agents/backstage')
		def defaultVersion = project.providers.provider {
			def properties = new Properties()

			AgentDocsPlugin.class.getResourceAsStream('/backstage.properties').withCloseable {
				properties.load(it)
			}

			return properties.getProperty('backstageVersion')
		}

		extension.version.convention(project.providers.gradleProperty('backstageVersion').orElse(defaultVersion))
		extension.outputDirectory.convention(defaultDirectory)

		def configuration = project.configurations.create('agentDocs') {
			canBeConsumed = false
			canBeResolved = true
			transitive = false
			description = "Backstage agent documentation"
		}

		configuration.defaultDependencies { dependencies ->
			dependencies.add(project.dependencies.create("com.backstage:agents:${extension.version.get()}"))
		}

		def archives = archiveOperations
		def fileSystem = fileSystemOperations
		def rootDirectory = project.rootDir.canonicalFile
		def agentsFile = new File(rootDirectory, 'AGENTS.md')
		def skillsDirectory = new File(rootDirectory, '.agents/skills')
		def legacyDirectories = [new File(rootDirectory, LEGACY_OUTPUT_DIRECTORY),
				project.layout.projectDirectory.dir(LEGACY_OUTPUT_DIRECTORY).asFile].unique()

		project.afterEvaluate {
			def outputDirectory = extension.outputDirectory.get().asFile
			def content = agentsFile.exists() ? agentsFile.getText('UTF-8') : ''

			def documentation = configuration.collect { jar ->
				def files = archives.zipTree(jar).matching {
					include '**/*.md', 'skills/**'
					exclude "**/$MANAGED_MARKER"
				}
				return new Documentation(files, jar.name, '', '')
			}
			def catalogName = CATALOG_NAMES.find { name ->
				documentation.any { source -> !source.files.matching { include(name) }.isEmpty() }
			}

			if (catalogName == null)
			{
				throw new GradleException('Backstage agent documentation must contain README.md or index.md at the archive root.')
			}

			def libraries = AgentDocsPlugin.collectLibraryDocumentation(project, archives)
			def catalog = new File(outputDirectory, libraries ? LIBRARY_CATALOG : catalogName)
			def updatedContent = AgentDocsPlugin.getUpdatedAgentsContent(content, agentsFile, catalog)
			AgentDocsPlugin.validateOutputDirectory(outputDirectory, defaultDirectory.asFile, rootDirectory, content)

			def sources = documentation + libraries
			def skills = AgentDocsPlugin.collectSkills(sources)
			def localSkillsDirectory = skillsDirectory.canonicalFile == skillsDirectory
			if (skills && !localSkillsDirectory)
			{
				throw new GradleException("Cannot install Backstage skills: $skillsDirectory points to another location.")
			}

			AgentDocsPlugin.validateSkillTargets(skillsDirectory, skills.keySet())
			AgentDocsPlugin.markManagedDirectory(outputDirectory)

			fileSystem.sync {
				sources.each { source ->
					from(source.files) {
						filteringCharset = 'UTF-8'
						if (libraries)
						{
							exclude(LIBRARY_CATALOG)
						}
						eachFile { details ->
							def resource = source.resource(details.relativeSourcePath.pathString)
							if (AgentDocsPlugin.isSkillResource(source, resource, skills))
							{
								details.exclude()
							}
							else
							{
								details.path = source.destinationPrefix + resource
								AgentDocsMarkdownLinks.relocate(details, resource, new File(outputDirectory, details.path)) { target ->
									AgentDocsPlugin.getInstalledFile(source, target, outputDirectory, skillsDirectory, skills)
								}
							}
						}
					}
				}

				into(outputDirectory)
				includeEmptyDirs = false
				preserve {
					include(MANAGED_MARKER)
					if (libraries)
					{
						include(LIBRARY_CATALOG)
					}
				}
			}

			if (libraries)
			{
				AgentDocsPlugin.writeLibraryCatalog(catalog, catalogName, libraries, skillsDirectory, skills)
			}

			if (localSkillsDirectory)
			{
				AgentDocsPlugin.installSkills(fileSystem, outputDirectory, skillsDirectory, skills)
			}

			if (updatedContent != content)
			{
				agentsFile.write(updatedContent, 'UTF-8')
			}

			AgentDocsPlugin.ignoreAgentsDirectory(rootDirectory)
			AgentDocsPlugin.removeLegacyDocumentation(fileSystem, legacyDirectories, outputDirectory)

			// Учитываем созданные файлы при проверке актуальности configuration cache.
			AgentDocsPlugin.trackInstalledFiles(project, outputDirectory)
			skills.keySet().each { name ->
				AgentDocsPlugin.trackInstalledFiles(project, new File(skillsDirectory, name))
			}
			agentsFile.getText('UTF-8')
		}
	}

	private static List<Documentation> collectLibraryDocumentation(Project project, ArchiveOperations archives)
	{
		def runtimeClasspath = project.configurations.findByName('runtimeClasspath')
		if (runtimeClasspath == null)
		{
			return []
		}

		def view = runtimeClasspath.incoming.artifactView {
			componentFilter { it instanceof ModuleComponentIdentifier }
		}
		List<Documentation> libraries = []

		view.artifacts.artifacts.sort { it.id.displayName }.each { artifact ->
			if (artifact.file.name.endsWith('.jar'))
			{
				def files = archives.zipTree(artifact.file).matching {
					include "${RESOURCE_PREFIX}**"
					exclude "**/$MANAGED_MARKER"
				}
				if (!files.isEmpty())
				{
					def component = artifact.id.componentIdentifier as ModuleComponentIdentifier
					libraries.add(new Documentation(files, artifact.id.displayName, RESOURCE_PREFIX, "${component.group}/${component.module}/"))
				}
			}
		}

		return libraries
	}

	private static Map<String, Documentation> collectSkills(List<Documentation> documentation)
	{
		Map<String, Documentation> skills = new TreeMap<>()

		documentation.each { source ->
			source.files.matching { include "${source.resourcePrefix}skills/*/SKILL.md" }.visit { details ->
				if (!details.directory)
				{
					def name = details.relativePath.segments[-2]
					def previous = skills.putIfAbsent(name, source)
					if (previous != null)
					{
						throw new GradleException("Cannot install Backstage skill '$name': it is provided by both ${previous.artifact} and ${source.artifact}.")
					}
				}
			}
		}

		return skills
	}

	private static void writeLibraryCatalog(File catalog, String backstageCatalog, List<Documentation> libraries,
											File skillsDirectory, Map<String, Documentation> skills)
	{
		def lines = [
				'# Документация библиотек',
				'',
				"- [Компоненты Backstage](<$backstageCatalog>)"
		]

		libraries.each { source ->
			def libraryDirectory = new File(catalog.parentFile, source.destinationPrefix)
			def readme = new File(libraryDirectory, 'README.md')
			if (libraryDirectory.isDirectory())
			{
				def link = readme.isFile() ? source.destinationPrefix + 'README.md' : source.destinationPrefix
				lines.add("- [${source.artifact}](<$link>)")
			}
			else
			{
				lines.add("- ${source.artifact}")
			}
			skills.findAll { name, owner -> owner == source }.keySet().each { name ->
				def link = relativePath(catalog.parentFile, new File(skillsDirectory, "$name/SKILL.md"))
				lines.add("  - [${name}](<$link>)")
			}
		}

		def content = lines.join('\n') + '\n'
		if (!catalog.exists() || catalog.getText('UTF-8') != content)
		{
			catalog.write(content, 'UTF-8')
		}
	}

	private static void trackInstalledFiles(Project project, File directory)
	{
		project.fileTree(directory).each { file ->
			project.providers.fileContents(project.layout.projectDirectory.file(file.absolutePath)).asBytes.get()
		}
	}

	private static void validateOutputDirectory(File directory, File defaultDirectory, File rootDirectory, String content)
	{
		def path = directory.canonicalFile.toPath()
		def requestedPath = directory.toPath().toAbsolutePath().normalize()
		def defaultPath = defaultDirectory.toPath().toAbsolutePath().normalize()
		def rootPath = rootDirectory.toPath()
		def agentsPath = rootPath.resolve('.agents')
		def insideAgents = requestedPath.startsWith(agentsPath) || path.startsWith(agentsPath)
		if (Files.isSymbolicLink(directory.toPath()) || rootPath.startsWith(path)
				|| (path != defaultPath && defaultPath.startsWith(path))
				|| (insideAgents && (path != defaultPath || requestedPath != defaultPath))
				|| path.startsWith(rootPath.resolve('.git')))
		{
			throw new GradleException("Invalid Backstage agent documentation output directory: $directory. Choose a dedicated directory.")
		}

		if (!directory.exists() || (directory.isDirectory() && directory.list()?.length == 0) || isManagedDirectory(directory))
		{
			return
		}

		// Распознаём прежнюю установку по каталогу и ссылке в AGENTS.md.
		def start = content.indexOf(BLOCK_START)
		def end = content.indexOf(BLOCK_END)
		def previousBlock = start >= 0 ? content.substring(start, end) : ''
		def previousInstallation = (CATALOG_NAMES + [LIBRARY_CATALOG]).any { name ->
			def catalog = new File(directory, name)
			catalog.isFile() && previousBlock.contains("](<${relativePath(rootDirectory, catalog)}>)")
		}

		if (!previousInstallation)
		{
			throw new GradleException("Cannot install Backstage agent documentation: $directory is not an empty or managed directory.")
		}
	}

	private static void validateSkillTargets(File skillsDirectory, Set<String> skillNames)
	{
		skillNames.each { name ->
			def target = new File(skillsDirectory, name)

			if (Files.exists(target.toPath(), LinkOption.NOFOLLOW_LINKS) && !isManagedDirectory(target))
			{
				throw new GradleException("Cannot install Backstage skill: $target already exists and is not managed by backstage-agent-docs.")
			}
		}
	}

	private static void installSkills(FileSystemOperations fileSystem, File documentationDirectory, File skillsDirectory, Map<String, Documentation> skills)
	{
		skillsDirectory.listFiles()?.findAll { isManagedDirectory(it) && !skills.containsKey(it.name) }
				?.each { directory -> fileSystem.delete { delete(directory) } }

		skills.each { name, source ->
			def target = new File(skillsDirectory, name)
			markManagedDirectory(target)

			def prefix = "${source.resourcePrefix}skills/$name/"
			fileSystem.sync {
				from(source.files) {
					include "${prefix}**"
					filteringCharset = 'UTF-8'
					eachFile { details ->
						def path = details.relativeSourcePath.pathString
						details.path = path.substring(prefix.length())
						AgentDocsMarkdownLinks.relocate(details, source.resource(path), new File(target, details.path)) { resource ->
							AgentDocsPlugin.getInstalledFile(source, resource, documentationDirectory, skillsDirectory, skills)
						}
					}
				}
				into(target)
				includeEmptyDirs = false
				preserve { include(MANAGED_MARKER) }
			}
		}
	}

	private static boolean isSkillResource(Documentation source, String resource, Map<String, Documentation> skills)
	{
		def segments = resource.split('/', 3)
		return segments.length >= 2 && segments[0] == 'skills' && skills.get(segments[1]) == source
	}

	private static File getInstalledFile(Documentation source, String resource, File outputDirectory, File skillsDirectory,
										 Map<String, Documentation> skills)
	{
		if (resource == 'skills' && skills.containsValue(source))
		{
			return skillsDirectory
		}

		if (isSkillResource(source, resource, skills))
		{
			return new File(skillsDirectory, resource.substring('skills/'.length()))
		}

		return new File(outputDirectory, source.destinationPrefix + resource)
	}

	private static void ignoreAgentsDirectory(File rootDirectory)
	{
		def gitignore = new File(rootDirectory, '.gitignore')
		if (!gitignore.isFile() || Files.isSymbolicLink(gitignore.toPath()))
		{
			return
		}

		def content = gitignore.getText('UTF-8')
		if (content.replaceFirst(/^\uFEFF/, '').readLines().any { it.stripTrailing() ==~ /(?:\/|\*\*\/)?\.agents(?:\/\*{0,2})?/ })
		{
			return
		}

		def newline = content.contains('\r\n') ? '\r\n' : '\n'
		def separator = content && !content.endsWith('\n') ? newline : ''
		gitignore.append(separator + '/.agents/' + newline, 'UTF-8')
		gitignore.getText('UTF-8')
	}

	private static void removeLegacyDocumentation(FileSystemOperations fileSystem, List<File> directories, File outputDirectory)
	{
		Path outputPath = outputDirectory.canonicalFile.toPath()
		directories.each { directory ->
			def path = directory.toPath().toAbsolutePath().normalize()
			if (path == directory.canonicalFile.toPath() && !outputPath.startsWith(path) && isManagedDirectory(directory))
			{
				fileSystem.delete { delete(directory) }
			}
		}
	}

	private static boolean isManagedDirectory(File directory)
	{
		return Files.isDirectory(directory.toPath(), LinkOption.NOFOLLOW_LINKS)
				&& Files.isRegularFile(new File(directory, MANAGED_MARKER).toPath(), LinkOption.NOFOLLOW_LINKS)
	}

	private static void markManagedDirectory(File directory)
	{
		if (!isManagedDirectory(directory))
		{
			Files.createDirectories(directory.toPath())
			Files.createFile(new File(directory, MANAGED_MARKER).toPath())
		}
	}

	private static String relativePath(File directory, File file)
	{
		return directory.toPath().relativize(file.toPath().normalize())
				.toString()
				.replace('\\', '/')
	}

	private static String getUpdatedAgentsContent(String content, File agentsFile, File catalog)
	{
		def lineSeparator = content.contains('\r\n') ? '\r\n' : '\n'
		def indexPath = relativePath(agentsFile.parentFile, catalog)

		def block = [
				BLOCK_START,
				'## Компоненты Backstage',
				'',
				'При работе с подключёнными библиотеками выбери подходящий skill или справочник.',
				'',
				"- [Каталог документации](<$indexPath>) содержит ссылки на материалы по модулям.",
				BLOCK_END
		].join(lineSeparator)

		def start = content.indexOf(BLOCK_START)
		def end = content.indexOf(BLOCK_END)
		def updatedContent

		if (start >= 0 || end >= 0)
		{
			if (start < 0 || end < start
					|| content.indexOf(BLOCK_START, start + BLOCK_START.length()) >= 0
					|| content.indexOf(BLOCK_END, end + BLOCK_END.length()) >= 0)
			{
				throw new GradleException("Cannot update $agentsFile: expected a single complete Backstage documentation block.")
			}

			updatedContent = content.substring(0, start) + block + content.substring(end + BLOCK_END.length())
		}
		else
		{
			def separator = ''

			if (!content.isEmpty() && !content.endsWith(lineSeparator + lineSeparator))
			{
				separator = content.endsWith(lineSeparator) ? lineSeparator : lineSeparator + lineSeparator
			}

			updatedContent = content + separator + block + lineSeparator
		}

		return updatedContent
	}

	@TupleConstructor(defaults = false)
	private static class Documentation
	{
		final FileTree files
		final String artifact
		final String resourcePrefix
		final String destinationPrefix

		String resource(String archivePath)
		{
			return archivePath.substring(resourcePrefix.length())
		}
	}
}

abstract class AgentDocsPluginExtension
{
	abstract Property<String> getVersion()

	abstract DirectoryProperty getOutputDirectory()
}
