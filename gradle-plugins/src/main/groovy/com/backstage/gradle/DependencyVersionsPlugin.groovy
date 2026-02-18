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

import groovy.xml.XmlSlurper
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

class DependencyVersionsPlugin implements Plugin<Project> {
	def libraries = [
			apachePOIVersion            : [group: "org.apache.poi", artifact: "poi"],
			camundaVersion              : [group: "org.camunda.bpm", artifact: "camunda-engine"],
			clickhouseJdbcVersion       : [group: "com.clickhouse", artifact: "clickhouse-jdbc"],
			commonsIOVersion            : [group: "commons-io", artifact: "commons-io"],
			commonsCodecVersion         : [group: "commons-codec", artifact: "commons-codec"],
			commonsCollections4Version  : [group: "org.apache.commons", artifact: "commons-collections4"],
			commonsTextVersion          : [group: "org.apache.commons", artifact: "commons-text"],
			commonsCSVVersion           : [group: "org.apache.commons", artifact: "commons-csv"],
			eclipseLinkVersion          : [group: "org.eclipse.persistence", artifact: "eclipselink"],
			flywayVersion               : [group: "org.flywaydb", artifact: "flyway-core"],
			flywayClickhouseVersion     : [group: "org.flywaydb", artifact: "flyway-database-clickhouse"],
			geojsonVersion              : [group: "de.grundid.opendatalab", artifact: "geojson-jackson"],
			groovyVersion               : [group: "org.apache.groovy", artifact: "groovy"],
			guavaVersion                : [group: "com.google.guava", artifact: "guava"],
			jetbrainsAnnotationsVersion : [group: "org.jetbrains", artifact: "annotations"],
			jooqVersion                 : [group: "org.jooq", artifact: "jooq"],
			jparsecVersion              : [group: "org.jparsec", artifact: "jparsec"],
			nashornVersion              : [group: "org.openjdk.nashorn", artifact: "nashorn-core"],
			minioVersion                : [group: "io.minio", artifact: "minio"],
			postgresqlJdbcVersion       : [group: "org.postgresql", artifact: "postgresql"],
			postgisVersion              : [group: "net.postgis", artifact: "postgis-jdbc"],
			springBootVersion           : [group: "org.springframework.boot", artifact: "spring-boot"],
			springCloudVersion          : [group: "org.springframework.cloud", artifact: "spring-cloud-starter-openfeign"],
			springDocVersion            : [group: "org.springdoc", artifact: "springdoc-openapi-starter-webmvc-ui"],

			jupiterVersion              : [group: "org.junit.jupiter", artifact: "junit-jupiter-api"],
	]

	@Override
	void apply(Project project) {
		project.tasks.register("updateDependencyVersions", {
			group = 'backstage'
			description = "Updates dependency versions (keeps same major) in gradle.properties"

			doLast {
				def propertiesFile = project.file("gradle.properties")

				if (!propertiesFile.exists())
				{
					throw new GradleException("gradle.properties not found")
				}

				// -------- helpers --------

				def parseVersion = { String v ->
					v.tokenize('.')
							.collect { it.replaceAll(/[^0-9]/, "") }
							.findAll { it }
							.collect { it as int }
				}

				def compareVersions = { List<Integer> a, List<Integer> b ->
					def max = Math.max(a.size(), b.size())
					for (int i = 0; i < max; i++) {
						def ai = i < a.size() ? a[i] : 0
						def bi = i < b.size() ? b[i] : 0
						if (ai != bi) return ai <=> bi
					}

					return 0
				}

				def readPropertyValue = { File file, String key ->
					file.readLines("UTF-8")
							.find { line ->
								def t = line.trim()
								!t.startsWith("#") && !t.startsWith("!") && t.startsWith(key)
							}
							?.split("=", 2)
							?.last()
							?.trim()
				}

				def updatePropertyPreservingFormat = { File file, Map<String, String> updates ->
					def lines = file.readLines("UTF-8")

					def updatedLines = lines.collect { line ->
						def trimmed = line.trim()

						if (trimmed.startsWith("#") || trimmed.startsWith("!") || !trimmed.contains("="))
						{
							return line
						}

						def idx = line.indexOf("=")
						def key = line.substring(0, idx).trim()

						if (updates.containsKey(key))
						{
							def prefix = line.substring(0, idx + 1)
							return prefix + updates[key]
						}

						return line
					}

					file.write(updatedLines.join(System.lineSeparator()), "UTF-8")
				}

				// -------- processing --------

				def updates = [:]
				def updated = false

				libraries.each { propertyName, coords ->
					def currentVersion = readPropertyValue(propertiesFile, propertyName)

					if (!currentVersion)
					{
						println "⚠️  $propertyName not found, skipping"

						return
					}

					def currentParsed = parseVersion(currentVersion)

					if (currentParsed.isEmpty())
					{
						println "⚠️  Cannot parse current version for $propertyName"

						return
					}

					def currentMajor = currentParsed[0]

					println "\n▶ Checking $propertyName ($currentVersion)"

					def metadataUrl =
							"https://repo1.maven.org/maven2/" +
									"${coords.group.replace('.', '/')}/" +
									"${coords.artifact}/maven-metadata.xml"

					def metadata

					try
					{
						metadata = new XmlSlurper().parse(metadataUrl)
					}
					catch (Exception e)
					{
						println "⚠️  Failed to load metadata: $metadataUrl. Reason: ${e.toString()}"

						return
					}

					def allVersions = metadata.versioning.versions.version*.text()

					def majorAvailable

					def compatibleVersions = allVersions.findAll { v ->
						def ver = parseVersion(v)

						if ((v.contains("-jre") || !v.contains("-")) && ver)                   // убираем rc / beta / SNAPSHOT
						{
							if ((!majorAvailable && ver[0] > currentMajor) || (majorAvailable && compareVersions(majorAvailable, ver)))
							{
								majorAvailable = ver
							}

							ver[0] == currentMajor
						}
					}

					if (majorAvailable)
					{
						println "   ❓ New major version found: ${majorAvailable.join('.')}"
					}

					if (compatibleVersions.isEmpty())
					{
						println "⚠️  No compatible versions found"

						return
					}

					def latestCompatible = compatibleVersions.max { a, b ->
						compareVersions(parseVersion(a), parseVersion(b))
					}

					println "   Latest compatible version: $latestCompatible"

					if (compareVersions(parseVersion(latestCompatible), currentParsed) > 0)
					{
						println "   ✅ Updating $propertyName → $latestCompatible"
						updates[propertyName] = latestCompatible

						updated = true
					}
					else
					{
						println "   ✔ Already up to date"
					}
				}

				// -------- write back --------

				if (updated)
				{
					updatePropertyPreservingFormat(propertiesFile, updates)

					println "\n🎉 gradle.properties updated"
				}
				else
				{
					println "\n👌 No updates required"
				}
			}
		})
	}
}

