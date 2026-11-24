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

import org.gradle.api.file.FileCopyDetails

import java.nio.file.Path
import java.util.regex.Pattern

final class AgentDocsMarkdownLinks
{
	private static final Pattern FENCE = ~/^ {0,3}(\x60{3,}|~{3,})(.*)$/
	private static final Pattern LINK = ~/(\x60+).*?\1|(\]\([ \t]*|^ {0,3}\[[^\]]+\]:[ \t]*)(?:<([^<>]+)>|((?:[^\s()<>]|\([^\s()<>]*\))+))/

	static void relocate(FileCopyDetails details, String resource, File installedFile, Closure<File> locate)
	{
		if (!resource.endsWith('.md'))
		{
			return
		}

		def source = Path.of(resource)
		def directory = installedFile.parentFile.toPath().toAbsolutePath().normalize()
		String fence

		details.filter { String line ->
			def marker = FENCE.matcher(line)
			if (marker.matches())
			{
				def delimiter = marker.group(1)
				if (fence == null)
				{
					fence = delimiter
				}
				else if (delimiter.startsWith(fence) && marker.group(2).isBlank())
				{
					fence = null
				}
				return line
			}

			if (fence != null)
			{
				return line
			}

			return line.replaceAll(LINK) { match, code, prefix, bracketed, plain ->
				if (prefix == null)
				{
					return match
				}

				def destination = relocateDestination(bracketed ?: plain, source, directory, locate)
				return bracketed == null ? prefix + destination : prefix + '<' + destination + '>'
			}
		}
	}

	private static String relocateDestination(String destination, Path source, Path directory, Closure<File> locate)
	{
		try
		{
			def uri = URI.create(destination.replace(' ', '%20'))
			def path = uri.path
			if (!path || uri.absolute || uri.rawAuthority != null || path.startsWith('/'))
			{
				return destination
			}

			def resource = source.resolveSibling(path).normalize()
			if (resource.startsWith('..'))
			{
				return destination
			}

			def target = locate(resource.toString().replace('\\', '/')).toPath().toAbsolutePath().normalize()
			if (target == directory.resolve(path).normalize())
			{
				return destination
			}

			def relative = directory.relativize(target).toString().replace('\\', '/')
			if (path.endsWith('/'))
			{
				relative = relative ? relative + '/' : './'
			}
			def link = new URI(null, null, relative, null).toString()
			def suffix = destination.find(/[?#].*$/) ?: ''
			return link + suffix
		}
		catch (IllegalArgumentException ignored)
		{
			return destination
		}
	}
}
