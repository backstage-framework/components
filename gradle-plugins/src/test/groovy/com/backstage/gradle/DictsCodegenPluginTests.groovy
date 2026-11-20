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

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime

import static org.junit.jupiter.api.Assertions.*

class DictsCodegenPluginTests
{
	private static final FileTime ORIGINAL_TIME = FileTime.fromMillis(1577836800000L)
	private static final String GENERATED_PACKAGE = 'client/src/main/java/example/generated'

	@TempDir
	Path projectDir

	@BeforeEach
	void init()
	{
		writeFile('settings.gradle', "rootProject.name = 'dicts-codegen-test'\ninclude 'client'\n")
		Files.createDirectory(projectDir.resolve('client'))
		writeFile('build.gradle', '''
			plugins {
				id 'java'
				id 'backstage-dicts-codegen'
			}

			project(':client') {
				apply plugin: 'java'
			}

			tasks.register('bootRun', JavaExec) {
				mainClass = file('GenerateDicts.java').absolutePath
			}

			dictsCodegen {
				targetProject = project(':client')
				if (providers.gradleProperty('testFormatter').present) {
					intellijFormatterCommand = [new File(System.getProperty('java.home'), 'bin/java').path, file('FormatDicts.java').absolutePath]
				}
			}
		'''.stripIndent())
		writeFile('.editorconfig', '''
			root = true

			[client/src/main/java/example/generated/*.java]
			ij_java_class_brace_style = next_line
		'''.stripIndent())
		writeFile('client/src/main/java/example/.editorconfig', '[*.java]\nindent_style = tab\n')
		writeFile('schema-version.txt', '1')
		writeFile('GenerateDicts.java', '''
			import java.nio.file.Files;
			import java.nio.file.Path;

			class GenerateDicts {
				public static void main(String[] args) throws Exception {
					var output = Path.of(System.getenv("app.dicts.codegen.outputPath")).resolve("example/generated");
					var version = Files.readString(Path.of("schema-version.txt"));
					Files.createDirectories(output);
					Files.writeString(output.resolve("Items.java"), "public class Items { int version = " + version + "; }\\n");
					Files.writeString(output.resolve("ItemsService.java"), "public class ItemsService { int value = 1; }\\n");
				}
			}
		'''.stripIndent())
		writeFile('FormatDicts.java', '''
			import java.nio.file.Files;
			import java.nio.file.Path;

			class FormatDicts {
				public static void main(String[] args) throws Exception {
					var sources = Path.of(args[args.length - 1]);
					var project = sources.getParent().getParent().getParent().getParent();
					if (!Files.readString(project.resolve(".editorconfig")).contains("[client/src/main/java/example/generated/*.java]") ||
							!Files.readString(sources.resolve("example/.editorconfig")).contains("indent_style = tab") ||
							!Files.isRegularFile(Path.of(System.getenv("IDEA_PROPERTIES")))) {
						throw new AssertionError("The staged sources have no EditorConfig hierarchy or formatter profile");
					}
					try (var paths = Files.walk(sources)) {
						var javaFiles = paths.filter(path -> path.toString().endsWith(".java")).toList();
						if (javaFiles.size() != 2) {
							throw new AssertionError("Expected a single formatter invocation for both generated classes");
						}
						for (var source : javaFiles) {
							Files.writeString(source, Files.readString(source).replace(" { ", "\\n{\\n\\t").replace(" }\\n", "\\n}\\n"));
						}
					}
				}
			}
		'''.stripIndent())
	}

	@Test
	void writesOnlyChangedFormattedSources()
	{
		var model = projectDir.resolve("$GENERATED_PACKAGE/Items.java".toString())
		var service = projectDir.resolve("$GENERATED_PACKAGE/ItemsService.java".toString())
		var manual = writeFile("$GENERATED_PACKAGE/Manual.java".toString(), 'public class Manual {}\n')
		Files.setLastModifiedTime(manual, ORIGINAL_TIME)
		var runner = runner('-Didea.active=false', '-PtestFormatter', '--configuration-cache')

		runner.build()

		assertEquals('public class Items\n{\n\tint version = 1;\n}\n', Files.readString(model))
		assertEquals('public class ItemsService\n{\n\tint value = 1;\n}\n', Files.readString(service))
		Files.setLastModifiedTime(model, ORIGINAL_TIME)
		Files.setLastModifiedTime(service, ORIGINAL_TIME)

		runner.build()

		assertEquals(ORIGINAL_TIME, Files.getLastModifiedTime(model))
		assertEquals(ORIGINAL_TIME, Files.getLastModifiedTime(service))
		assertEquals(ORIGINAL_TIME, Files.getLastModifiedTime(manual))

		writeFile('schema-version.txt', '2')
		runner.build()

		assertEquals('public class Items\n{\n\tint version = 2;\n}\n', Files.readString(model))
		assertNotEquals(ORIGINAL_TIME, Files.getLastModifiedTime(model))
		assertEquals(ORIGINAL_TIME, Files.getLastModifiedTime(service))
	}

	@Test
	void skipsFormattingOutsideIntellijByDefault()
	{
		runner('-Didea.active=false').build()

		assertEquals('public class Items { int version = 1; }\n', Files.readString(projectDir.resolve("$GENERATED_PACKAGE/Items.java".toString())))
	}

	@Test
	@EnabledOnOs([OS.MAC, OS.LINUX])
	void findsIntellijFormatterByDefault()
	{
		var java = new File(System.getProperty('java.home'), 'bin/java').path
		var launcher = writeFile('IntelliJ IDEA/bin/format.sh', "#!/bin/sh\nexec \"$java\" \"${projectDir.resolve('FormatDicts.java')}\" \"\$@\"\n")
		assertTrue(launcher.toFile().setExecutable(true))

		runner('-Didea.active=true').withEnvironment(System.getenv() + [IDEA_HOME: launcher.parent.parent.toString()]).build()

		assertEquals('public class Items\n{\n\tint version = 1;\n}\n', Files.readString(projectDir.resolve("$GENERATED_PACKAGE/Items.java".toString())))
	}

	@Test
	void leavesSourcesIntactIfFormatterFails()
	{
		var model = writeFile("$GENERATED_PACKAGE/Items.java".toString(), 'public class Items {}\n')
		Files.setLastModifiedTime(model, ORIGINAL_TIME)
		writeFile('FormatDicts.java', 'class FormatDicts { public static void main(String[] args) { System.exit(3); } }\n')

		var result = runner('-Didea.active=false', '-PtestFormatter').buildAndFail()

		assertTrue(result.output.contains('IntelliJ formatter exited with code 3.'))
		assertEquals('public class Items {}\n', Files.readString(model))
		assertEquals(ORIGINAL_TIME, Files.getLastModifiedTime(model))
		assertFalse(Files.exists(projectDir.resolve("$GENERATED_PACKAGE/ItemsService.java".toString())))
	}

	private GradleRunner runner(String... arguments)
	{
		return GradleRunner.create()
				.withProjectDir(projectDir.toFile())
				.withPluginClasspath()
				.withArguments(['dictsCodegen', '--offline', '--stacktrace'] + arguments.toList())
	}

	private Path writeFile(String path, String text)
	{
		var file = projectDir.resolve(path)
		Files.createDirectories(file.parent)

		return Files.writeString(file, text)
	}
}
