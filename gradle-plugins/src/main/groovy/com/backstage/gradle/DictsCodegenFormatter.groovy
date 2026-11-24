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

import groovy.transform.CompileStatic
import groovy.transform.TupleConstructor
import org.gradle.api.GradleException
import org.gradle.api.tasks.JavaExec

import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.regex.Pattern

@CompileStatic
@TupleConstructor(defaults = false)
class DictsCodegenFormatter
{
	private static final String OUTPUT_PATH_PROPERTY = 'app.dicts.codegen.outputPath'

	final Path outputDirectory
	final List<String> command

	private Path workingDirectory

	void prepare(JavaExec task)
	{
		workingDirectory = Files.createTempDirectory(task.temporaryDir.toPath(), 'intellij-format-')
		var stagedSources = Files.createDirectories(stagedPath(workingDirectory, outputDirectory))
		task.environment(OUTPUT_PATH_PROPERTY, stagedSources.toString())
	}

	void formatAndWrite()
	{
		var workspace = workingDirectory
		var stagedSources = stagedPath(workspace, outputDirectory)

		try
		{
			List<Path> sources
			try (var paths = Files.walk(stagedSources))
			{
				sources = paths.filter { path -> Files.isRegularFile(path) && path.fileName.toString().endsWith('.java') }.toList()
			}

			if (copyEditorConfigs(workspace, stagedSources, sources))
			{
				format(workspace, stagedSources)
			}

			DictsCodegenImportsOptimizer.optimize(workspace, sources)

			sources.each { source -> writeIfChanged(source, outputDirectory.resolve(stagedSources.relativize(source))) }
		}
		finally
		{
			deleteWorkspace(workspace)
		}
	}

	static List<String> findIntellijFormatter()
	{
		if (!Boolean.getBoolean('idea.active'))
		{
			return []
		}

		var directories = new ArrayList<Path>()
		var ideaHome = System.getenv('IDEA_HOME')
		if (ideaHome)
		{
			directories.add(Path.of(ideaHome, 'bin'))
			directories.add(Path.of(ideaHome, 'Contents', 'bin'))
		}

		var userApplications = Path.of(System.getProperty('user.home'), 'Applications')
		for (var applications : [Path.of('/Applications'), userApplications])
		{
			directories.add(applications.resolve('IntelliJ IDEA.app/Contents/bin'))
			directories.add(applications.resolve('IntelliJ IDEA CE.app/Contents/bin'))
		}

		List<String> launcherNames = ['idea', 'idea.sh', 'idea64.exe']
		var launchers = new ArrayList<Path>()
		for (var directory : directories)
		{
			launchers.add(directory.resolve('format.sh'))
			launcherNames.each { name -> launchers.add(directory.resolve(name)) }
		}

		for (var directory : (System.getenv('PATH') ?: '').split(Pattern.quote(File.pathSeparator)))
		{
			if (directory)
			{
				try
				{
					launcherNames.each { name -> launchers.add(Path.of(directory, name)) }
				}
				catch (InvalidPathException ignored)
				{
					// Пропускаем некорректный путь к каталогу.
				}
			}
		}

		for (var launcher : launchers)
		{
			if (Files.isRegularFile(launcher) && Files.isExecutable(launcher))
			{
				return launcher.fileName.toString() == 'format.sh' ? [launcher.toString()] : [launcher.toString(), 'format']
			}
		}

		return []
	}

	private boolean copyEditorConfigs(Path workspace, Path stagedSources, List<Path> sources)
	{
		Files.writeString(workspace.resolve('.editorconfig'), 'root = true\n')
		var visited = new HashSet<Path>()
		var found = false

		for (var source : sources)
		{
			var destination = outputDirectory.resolve(stagedSources.relativize(source))
			for (var directory = destination.parent; directory != null && visited.add(directory); directory = directory.parent)
			{
				var config = directory.resolve('.editorconfig')
				if (Files.isRegularFile(config))
				{
					var stagedConfig = stagedPath(workspace, config)
					Files.createDirectories(stagedConfig.parent)
					Files.copy(config, stagedConfig)
					found = true
				}
			}
		}

		return found
	}

	private void format(Path workspace, Path stagedSources)
	{
		var profile = new Properties()
		for (var directory : ['config', 'system', 'plugins', 'log'])
		{
			profile.setProperty("idea.${directory}.path".toString(), workspace.resolve(directory).toString().replace('\\', '/'))
		}

		var properties = workspace.resolve('idea.properties')
		try (var output = Files.newOutputStream(properties))
		{
			profile.store(output, null)
		}

		var formatter = new ProcessBuilder(command + ['-allowDefaults', '-r', '-m', '*.java', stagedSources.toString()])
		formatter.environment().put('IDEA_PROPERTIES', properties.toString())
		var process = formatter.inheritIO().start()
		try
		{
			var exitCode = process.waitFor()
			if (exitCode != 0)
			{
				throw new GradleException("IntelliJ formatter exited with code $exitCode.")
			}
		}
		catch (InterruptedException e)
		{
			process.destroyForcibly()
			Thread.currentThread().interrupt()
			throw e
		}
	}

	private static void writeIfChanged(Path source, Path target)
	{
		if (Files.exists(target) && Files.mismatch(source, target) == -1L)
		{
			return
		}

		Files.createDirectories(target.parent)
		var temporary = Files.createTempFile(target.parent, '.codegen-', '.tmp')
		try
		{
			Files.copy(source, temporary, StandardCopyOption.REPLACE_EXISTING)
			try
			{
				Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
			}
			catch (AtomicMoveNotSupportedException ignored)
			{
				Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING)
			}
		}
		finally
		{
			Files.deleteIfExists(temporary)
		}
	}

	private static Path stagedPath(Path workspace, Path destination)
	{
		return workspace.resolve('sources').resolve(destination.root.relativize(destination))
	}

	private static void deleteWorkspace(Path workspace)
	{
		if (Files.exists(workspace))
		{
			try (var paths = Files.walk(workspace))
			{
				paths.sorted(Comparator.reverseOrder()).forEach { path -> Files.deleteIfExists(path) }
			}
		}
	}
}
