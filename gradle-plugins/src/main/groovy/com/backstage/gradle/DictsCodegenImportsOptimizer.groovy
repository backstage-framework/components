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

import com.sun.source.tree.ClassTree
import com.sun.source.tree.CompilationUnitTree
import com.sun.source.tree.IdentifierTree
import com.sun.source.util.JavacTask
import com.sun.source.util.SourcePositions
import com.sun.source.util.TreeScanner
import com.sun.source.util.Trees
import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.TupleConstructor

import javax.tools.Diagnostic
import javax.tools.DiagnosticCollector
import javax.tools.JavaCompiler
import javax.tools.JavaFileObject
import javax.tools.ToolProvider
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.util.regex.Pattern

/**
 * Консервативный оптимизатор импортов для промежуточных исходников, созданных кодогенерацией.
 * Использует компилятор JDK ({@link ToolProvider#getSystemJavaCompiler()}) и не требует IntelliJ Platform.
 * <p>
 * Поддерживает подмножество настройки {@code ij_java_imports_layout}: {@code @*}, {@code *}, {@code |}, {@code $*},
 * а также шаблоны пакетов ({@code package.*}, {@code package.**}), в том числе для статических импортов.
 * Явные импорты заменяются импортом по требованию, только если достигнут заданный порог и нет очевидной неоднозначности имён.
 * Типы без classpath не разрешаются, поэтому полное совпадение с результатом Optimize Imports в IntelliJ IDEA не гарантируется.
 */
@CompileStatic
class DictsCodegenImportsOptimizer
{
	private static final String IMPORTS_LAYOUT_KEY = 'ij_java_imports_layout'
	private static final String CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND_KEY = 'ij_java_class_count_to_use_import_on_demand'
	private static final String NAMES_COUNT_TO_USE_IMPORT_ON_DEMAND_KEY = 'ij_java_names_count_to_use_import_on_demand'
	private static final String USE_SINGLE_CLASS_IMPORTS_KEY = 'ij_java_use_single_class_imports'
	private static final String PACKAGES_TO_USE_IMPORT_ON_DEMAND_KEY = 'ij_java_packages_to_use_import_on_demand'

	private static final String DEFAULT_IMPORTS_LAYOUT = '*,|,javax.**,java.**,|,$*'
	private static final int DEFAULT_CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND = 5
	private static final int DEFAULT_NAMES_COUNT_TO_USE_IMPORT_ON_DEMAND = 3
	private static final String DEFAULT_USE_SINGLE_CLASS_IMPORTS = 'true'
	private static final String DEFAULT_PACKAGES_TO_USE_IMPORT_ON_DEMAND = ''

	private static final String LAYOUT_MODULE_IMPORTS = '@*'
	private static final String LAYOUT_OTHER_IMPORTS = '*'
	private static final String LAYOUT_STATIC_IMPORTS = '$*'
	private static final String LAYOUT_GROUP_SEPARATOR = '|'
	private static final String LAYOUT_STATIC_PREFIX = '$'
	private static final List<String> LAYOUT_GENERIC_TOKENS = [LAYOUT_MODULE_IMPORTS, LAYOUT_OTHER_IMPORTS, LAYOUT_STATIC_IMPORTS]
	private static final Pattern LAYOUT_WILDCARD_SUFFIX = Pattern.compile('\\.\\*\\*?$')
	private static final int LAYOUT_NO_MATCH = -1
	private static final int LAYOUT_GENERIC_MATCH = 0

	private static final String IMPORT_PREFIX = 'import '
	private static final String STATIC_IMPORT_PREFIX = 'import static '
	private static final String MODULE_IMPORT_PREFIX = 'import module '
	private static final String STATEMENT_END = ';'
	private static final String WILDCARD_SUFFIX = '.*'
	private static final String RECURSIVE_WILDCARD_SUFFIX = '.**'
	private static final String JAVA_LANG_PREFIX = 'java.lang.'
	private static final String NAME_SEPARATOR = '.'
	private static final String LIST_SEPARATOR = ','
	private static final List<String> JAVADOC_REFERENCE_TAGS = ['@link ', '@linkplain ', '@see ', '@throws ', '@value ']

	private static final String EDITOR_CONFIG_FILE = '.editorconfig'
	private static final String EDITOR_CONFIG_ROOT = 'root = true'
	private static final List<String> EDITOR_CONFIG_COMMENT_PREFIXES = ['#', ';']
	private static final String EDITOR_CONFIG_SECTION_START = '['
	private static final String EDITOR_CONFIG_SECTION_END = ']'
	private static final String EDITOR_CONFIG_VALUE_SEPARATOR = '='
	private static final String EDITOR_CONFIG_PATH_SEPARATOR = '/'
	private static final String GLOB_SYNTAX = 'glob:'

	private static final List<String> COMPILER_OPTIONS = ['-proc:none']
	private static final String LF = '\n'
	private static final String CRLF = '\r\n'

	/**
	 * Оптимизирует импорты только в промежуточных исходниках и не обращается к файлам целевого проекта.
	 * Файл {@code .editorconfig} должны быть заранее скопированы в рабочий каталог с сохранением относительных путей.
	 */
	static void optimize(Path workspace, List<Path> files)
	{
		var compiler = ToolProvider.systemJavaCompiler
		if (compiler == null)
		{
			throw new IllegalStateException('A JDK is required for Java import optimization (not a JRE).')
		}

		files.each { file -> optimizeFile(compiler, workspace, file) }
	}

	private static void optimizeFile(JavaCompiler compiler, Path workspace, Path file)
	{
		var source = Files.readString(file)
		var settings = readEditorConfig(workspace, file)
		var rules = parseLayout(settings.getOrDefault(IMPORTS_LAYOUT_KEY, DEFAULT_IMPORTS_LAYOUT))
		var diagnostics = new DiagnosticCollector<JavaFileObject>()

		try (var fileManager = compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8))
		{
			var task = compiler.getTask(null, fileManager, diagnostics, COMPILER_OPTIONS, null, fileManager.getJavaFileObjects(file)) as JavacTask
			var unit = task.parse().first()
			checkParsed(file, diagnostics)

			if (unit.imports.empty)
			{
				return
			}

			var block = readImportBlock(source, file, unit, Trees.instance(task).sourcePositions)
			var imports = collapseToOnDemand(retainRequired(block.imports, source, unit), settings, unit)
			var lineSeparator = source.contains(CRLF) ? CRLF : LF

			// Текст вокруг блока импортов не меняется: если удалены все импорты, пустые строки на его месте остаются.
			var rewritten = source.substring(0, block.start) + renderImports(imports, rules, lineSeparator) + source.substring(block.end)

			if (source != rewritten)
			{
				Files.writeString(file, rewritten)
			}
		}
	}

	private static void checkParsed(Path file, DiagnosticCollector<JavaFileObject> diagnostics)
	{
		var errors = diagnostics.diagnostics.findAll { diagnostic -> diagnostic.kind == Diagnostic.Kind.ERROR }

		if (!errors.empty)
		{
			throw new IllegalStateException("Cannot parse generated Java file $file:$LF${errors.join(LF)}")
		}
	}

	private static ImportBlock readImportBlock(String source, Path file, CompilationUnitTree unit, SourcePositions positions)
	{
		var imports = new ArrayList<ImportLine>()
		var blockStart = -1
		var previousEnd = -1

		for (var tree : unit.imports)
		{
			var start = Math.toIntExact(positions.getStartPosition(unit, tree))
			var end = Math.toIntExact(positions.getEndPosition(unit, tree))
			if (start < 0 || end <= start || end > source.length())
			{
				throw new IllegalStateException("Invalid import offsets in $file")
			}

			if (previousEnd < 0)
			{
				blockStart = start
			}
			else if (!source.substring(previousEnd, start).blank)
			{
				// Комментарии и любой другой текст между импортами терять нельзя.
				throw new IllegalStateException("Comment/non-whitespace between imports in $file")
			}

			var statement = source.substring(start, end).trim()
			imports.add(new ImportLine(statement, tree.qualifiedIdentifier.toString(), tree.isStatic(), statement.startsWith(MODULE_IMPORT_PREFIX)))
			previousEnd = end
		}

		return new ImportBlock(blockStart, previousEnd, imports)
	}

	private static List<ImportLine> retainRequired(List<ImportLine> imports, String source, CompilationUnitTree unit)
	{
		var usedIdentifiers = collectUsedIdentifiers(unit)

		return imports.findAll { item -> isRequired(item, usedIdentifiers, source) }
	}

	private static boolean isRequired(ImportLine item, Set<String> usedIdentifiers, String source)
	{
		if (item.moduleImport)
		{
			return true
		}

		// Классы java.lang доступны без импорта. Одноимённый класс из пакета самого файла изменил бы привязку имени,
		// но в сгенерированных исходниках такой конфликт не ожидается.
		if (item.name.startsWith(JAVA_LANG_PREFIX) && item.name.indexOf(NAME_SEPARATOR, JAVA_LANG_PREFIX.length()) < 0)
		{
			return false
		}

		// Без разрешения типов нельзя определить, используется ли импорт по требованию, поэтому он всегда сохраняется.
		return item.wildcard || usedIdentifiers.contains(item.simpleName) || isReferencedInJavadoc(source, item.simpleName)
	}

	private static Set<String> collectUsedIdentifiers(CompilationUnitTree unit)
	{
		var usedIdentifiers = new HashSet<String>()
		var scanner = new TreeScanner<Void, Set<String>>() {
			@Override
			Void visitIdentifier(IdentifierTree node, Set<String> identifiers)
			{
				identifiers.add(node.name.toString())

				// Идентификатор — лист дерева: обходить дальше нечего.
				return null
			}
		}

		scanner.scan(unit.typeDecls, usedIdentifiers)
		scanner.scan(unit.packageAnnotations, usedIdentifiers)

		return usedIdentifiers
	}

	/**
	 * Упрощённая проверка: импорт сохраняется, если его простое имя встречается после одного из тегов Javadoc.
	 * Лишний импорт безопаснее, чем сломанная ссылка в документации.
	 */
	private static boolean isReferencedInJavadoc(String source, String simpleName)
	{
		return JAVADOC_REFERENCE_TAGS.any { tag -> source.contains(tag + simpleName) }
	}

	/**
	 * Заменяет явные импорты импортами по требованию согласно порогам из EditorConfig.
	 * Обычные импорты группируются по пакету, статические — по классу, в котором объявлен импортируемый элемент.
	 * Явные импорты сохраняются везде, где безопасность замены можно доказать только разрешением типов.
	 * Уже существующие импорты по требованию не разворачиваются по той же причине.
	 */
	private static List<ImportLine> collapseToOnDemand(List<ImportLine> imports, Map<String, String> settings, CompilationUnitTree unit)
	{
		var classThreshold = readPositiveInt(settings, CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND_KEY, DEFAULT_CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND)
		var staticThreshold = readPositiveInt(settings, NAMES_COUNT_TO_USE_IMPORT_ON_DEMAND_KEY, DEFAULT_NAMES_COUNT_TO_USE_IMPORT_ON_DEMAND)

		if (!Boolean.parseBoolean(settings.getOrDefault(USE_SINGLE_CLASS_IMPORTS_KEY, DEFAULT_USE_SINGLE_CLASS_IMPORTS)))
		{
			classThreshold = 1
		}

		var collapsed = [findOnDemandPackageImport(imports, settings, unit, classThreshold), findOnDemandStaticImport(imports, staticThreshold)]
				.findAll { onDemand -> onDemand != null }

		if (collapsed.empty)
		{
			return imports
		}

		var result = new ArrayList<ImportLine>()
		var added = new HashSet<ImportLine>()

		for (var item : imports)
		{
			if (item.moduleImport || item.wildcard)
			{
				result.add(item)
				continue
			}

			var onDemand = ImportLine.onDemand(item.owner, item.staticImport)

			if (!collapsed.contains(onDemand))
			{
				result.add(item)
			}
			else if (added.add(onDemand))
			{
				result.add(onDemand)
			}
		}

		return result
	}

	private static ImportLine findOnDemandPackageImport(List<ImportLine> imports, Map<String, String> settings, CompilationUnitTree unit, int threshold)
	{
		// Импорт пакета по требованию может изменить то, какой тип победит при разрешении имени.
		// Без classpath его нельзя добавлять рядом с другим импортом по требованию или импортом модуля.
		if (imports.any { item -> item.moduleImport || !item.staticImport && item.wildcard })
		{
			return null
		}

		var packagesOnDemand = settings.getOrDefault(PACKAGES_TO_USE_IMPORT_ON_DEMAND_KEY, DEFAULT_PACKAGES_TO_USE_IMPORT_ON_DEMAND)
		var localTypes = collectTopLevelTypeNames(unit)
		var eligible = new ArrayList<String>()

		for (var group : groupByOwner(imports, false).entrySet())
		{
			var owner = group.key
			var members = group.value

			if (looksLikeType(owner) || countDistinctNames(members) < threshold && !isPackageOnDemand(packagesOnDemand, owner))
			{
				continue
			}

			var names = members*.simpleName.toSet()

			if (!collidesWithJavaLangOrLocalType(names, localTypes) && !collidesWithExplicitImport(imports, members, names))
			{
				eligible.add(owner)
			}
		}

		// Два новых импорта по требованию могут конфликтовать между собой, даже если имена их явных импортов
		// не пересекаются (например, java.util.List и java.awt.List), поэтому сворачивается только единственный пакет.
		return eligible.size() == 1 ? ImportLine.onDemand(eligible.first(), false) : null
	}

	private static ImportLine findOnDemandStaticImport(List<ImportLine> imports, int threshold)
	{
		// Статический импорт по требованию может сделать неоднозначным выбор перегруженного метода,
		// поэтому сворачиваются только статические импорты из единственного класса.
		if (imports.any { item -> item.staticImport && item.wildcard })
		{
			return null
		}

		var groups = groupByOwner(imports, true)

		if (groups.size() != 1)
		{
			return null
		}

		var group = groups.entrySet().first()

		return countDistinctNames(group.value) >= threshold ? ImportLine.onDemand(group.key, true) : null
	}

	private static Map<String, List<ImportLine>> groupByOwner(List<ImportLine> imports, boolean staticImport)
	{
		return imports
				.findAll { item -> !item.moduleImport && !item.wildcard && item.staticImport == staticImport }
				.groupBy { item -> item.owner }
	}

	private static int countDistinctNames(List<ImportLine> imports)
	{
		return imports*.name.toSet().size()
	}

	private static Set<String> collectTopLevelTypeNames(CompilationUnitTree unit)
	{
		var names = new HashSet<String>()

		for (var declaration : unit.typeDecls)
		{
			if (declaration instanceof ClassTree)
			{
				names.add(declaration.simpleName.toString())
			}
		}

		return names
	}

	/**
	 * Без разрешения типов вложенный класс нельзя отличить от пакета, поэтому владелец с заглавной буквы считается классом.
	 */
	private static boolean looksLikeType(String owner)
	{
		var lastSegment = owner.substring(owner.lastIndexOf(NAME_SEPARATOR) + 1)

		return !lastSegment.empty && Character.isUpperCase(lastSegment.charAt(0))
	}

	private static boolean isPackageOnDemand(String packagesOnDemand, String owner)
	{
		for (var rawPattern : packagesOnDemand.split(LIST_SEPARATOR))
		{
			var pattern = rawPattern.trim()

			if (pattern.endsWith(RECURSIVE_WILDCARD_SUFFIX))
			{
				var parent = pattern.substring(0, pattern.length() - RECURSIVE_WILDCARD_SUFFIX.length())

				if (owner == parent || owner.startsWith(parent + NAME_SEPARATOR))
				{
					return true
				}
			}
			else if (pattern.endsWith(WILDCARD_SUFFIX) && owner == pattern.substring(0, pattern.length() - WILDCARD_SUFFIX.length()))
			{
				return true
			}
		}

		return false
	}

	private static boolean collidesWithExplicitImport(List<ImportLine> imports, List<ImportLine> members, Set<String> names)
	{
		return imports.any { item ->
			!item.staticImport && !item.moduleImport && !item.wildcard && !members.contains(item) && names.contains(item.simpleName)
		}
	}

	private static boolean collidesWithJavaLangOrLocalType(Set<String> names, Set<String> localTypes)
	{
		return names.any { name -> localTypes.contains(name) || isJavaLangType(name) }
	}

	private static boolean isJavaLangType(String name)
	{
		try
		{
			// Класс загружается без инициализации. Если такое же имя есть в java.lang,
			// замена явного импорта импортом по требованию сделает ссылку на него неоднозначной.
			Class.forName(JAVA_LANG_PREFIX + name, false, DictsCodegenImportsOptimizer.classLoader)

			return true
		}
		catch (ClassNotFoundException ignored)
		{
			// В java.lang текущей JDK такого класса нет.
			return false
		}
		catch (LinkageError ignored)
		{
			// Отсутствие конфликта доказать не удалось, поэтому считаем, что он есть.
			return true
		}
	}

	private static int readPositiveInt(Map<String, String> settings, String key, int fallback)
	{
		try
		{
			var value = Integer.parseInt(settings.getOrDefault(key, String.valueOf(fallback)).trim())

			if (value < 1)
			{
				throw new NumberFormatException('threshold must be positive')
			}

			return value
		}
		catch (NumberFormatException e)
		{
			throw new IllegalArgumentException("Invalid .editorconfig setting $key: ${settings.get(key)}", e)
		}
	}

	/**
	 * Удаляет дубликаты, сортирует импорты согласно {@code ij_java_imports_layout} и разделяет группы пустой строкой.
	 */
	private static String renderImports(List<ImportLine> imports, List<LayoutRule> rules, String lineSeparator)
	{
		var layout = new LinkedHashMap<ImportLine, LayoutRule>()

		for (var item : imports)
		{
			layout.putIfAbsent(item, selectRule(item, rules))
		}

		// Правила пронумерованы в порядке раскладки, поэтому сортировка по номеру правила сохраняет и порядок групп.
		return layout.keySet()
				.toSorted { left, right -> layout[left].order <=> layout[right].order ?: left.name <=> right.name }
				.groupBy { item -> layout[item].group }
				.values()
				.collect { group -> group*.statement.join(lineSeparator) }
				.join(lineSeparator * 2)
	}

	private static LayoutRule selectRule(ImportLine item, List<LayoutRule> rules)
	{
		LayoutRule winner = null
		var bestScore = LAYOUT_NO_MATCH

		// При равной оценке выигрывает правило, объявленное раньше.
		for (var rule : rules)
		{
			var score = rule.score(item)
			if (score > bestScore)
			{
				bestScore = score
				winner = rule
			}
		}

		if (winner == null)
		{
			throw new IllegalStateException("No import layout rule for ${item.statement}")
		}

		return winner
	}

	private static List<LayoutRule> parseLayout(String layout)
	{
		var rules = new ArrayList<LayoutRule>()
		var group = 0

		for (var rawToken : layout.split(LIST_SEPARATOR))
		{
			var token = rawToken.trim()

			if (token == LAYOUT_GROUP_SEPARATOR)
			{
				group++
			}
			else if (!token.empty)
			{
				if (!LAYOUT_GENERIC_TOKENS.contains(token) && !token.endsWith(RECURSIVE_WILDCARD_SUFFIX) && !token.endsWith(WILDCARD_SUFFIX))
				{
					throw new IllegalArgumentException("Unsupported $IMPORTS_LAYOUT_KEY token: $token")
				}

				rules.add(new LayoutRule(token, group, rules.size()))
			}
		}

		if (rules.empty)
		{
			throw new IllegalArgumentException('Empty import layout')
		}

		return rules
	}

	private static Map<String, String> readEditorConfig(Path workspace, Path javaFile)
	{
		if (!javaFile.toAbsolutePath().normalize().startsWith(workspace.toAbsolutePath().normalize()))
		{
			throw new IllegalArgumentException("Staged source is outside workspace: $javaFile")
		}

		var directories = new ArrayList<Path>()

		for (var directory = javaFile.parent; directory != null && directory.startsWith(workspace); directory = directory.parent)
		{
			directories.add(directory)
		}

		// Настройки применяются от рабочего каталога к каталогу файла: ближайший .editorconfig имеет приоритет.
		var settings = new HashMap<String, String>()

		for (var directory : directories.reverse())
		{
			var config = directory.resolve(EDITOR_CONFIG_FILE)
			if (Files.isRegularFile(config))
			{
				applyEditorConfig(config, javaFile, settings)
			}
		}

		return settings
	}

	private static void applyEditorConfig(Path config, Path javaFile, Map<String, String> settings)
	{
		var activeSection = false

		for (var rawLine : Files.readAllLines(config))
		{
			var line = rawLine.trim()

			if (line.empty || EDITOR_CONFIG_COMMENT_PREFIXES.any { prefix -> line.startsWith(prefix) })
			{
				continue
			}

			if (line.startsWith(EDITOR_CONFIG_SECTION_START) && line.endsWith(EDITOR_CONFIG_SECTION_END))
			{
				var pattern = line.substring(EDITOR_CONFIG_SECTION_START.length(), line.length() - EDITOR_CONFIG_SECTION_END.length()).trim()
				activeSection = matchesSection(pattern, config.parent, javaFile)
			}
			else if (!activeSection && line.equalsIgnoreCase(EDITOR_CONFIG_ROOT))
			{
				// Корневой .editorconfig отменяет настройки вышестоящих каталогов.
				settings.clear()
			}
			else if (activeSection)
			{
				var separator = line.indexOf(EDITOR_CONFIG_VALUE_SEPARATOR)
				if (separator > 0)
				{
					settings.put(line.substring(0, separator).trim().toLowerCase(Locale.ROOT), line.substring(separator + 1).trim())
				}
			}
		}
	}

	private static boolean matchesSection(String pattern, Path configDirectory, Path javaFile)
	{
		try
		{
			// Шаблон без разделителя пути сопоставляется только с именем файла.
			var subject = pattern.contains(EDITOR_CONFIG_PATH_SEPARATOR) ? configDirectory.relativize(javaFile) : javaFile.fileName
			var glob = pattern.startsWith(EDITOR_CONFIG_PATH_SEPARATOR) ? pattern.substring(EDITOR_CONFIG_PATH_SEPARATOR.length()) : pattern

			return FileSystems.getDefault().getPathMatcher(GLOB_SYNTAX + glob).matches(subject)
		}
		catch (RuntimeException e)
		{
			throw new IllegalArgumentException("Unsupported .editorconfig section [$pattern]", e)
		}
	}

	/**
	 * Блок импортов файла: границы в исходном тексте и импорты в порядке их объявления.
	 */
	@TupleConstructor(defaults = false)
	private static class ImportBlock
	{
		final int start
		final int end
		final List<ImportLine> imports
	}

	/**
	 * Импорт: исходный текст объявления и полное импортируемое имя.
	 */
	@EqualsAndHashCode
	@TupleConstructor(defaults = false)
	private static class ImportLine
	{
		final String statement
		final String name
		final boolean staticImport
		final boolean moduleImport

		static ImportLine onDemand(String owner, boolean staticImport)
		{
			var name = owner + WILDCARD_SUFFIX

			return new ImportLine((staticImport ? STATIC_IMPORT_PREFIX : IMPORT_PREFIX) + name + STATEMENT_END, name, staticImport, false)
		}

		boolean isWildcard()
		{
			return name.endsWith(WILDCARD_SUFFIX)
		}

		String getSimpleName()
		{
			return name.substring(name.lastIndexOf(NAME_SEPARATOR) + 1)
		}

		String getOwner()
		{
			return name.substring(0, name.lastIndexOf(NAME_SEPARATOR))
		}
	}

	/**
	 * Правило раскладки импортов: шаблон, номер группы, отделяемой пустой строкой, и порядковый номер правила.
	 */
	@TupleConstructor(defaults = false)
	private static class LayoutRule
	{
		final String pattern
		final int group
		final int order

		/**
		 * Возвращает оценку соответствия импорта правилу: чем длиннее совпавший префикс пакета, тем она выше.
		 * Общие шаблоны получают наименьшую оценку, поэтому правило для конкретного пакета всегда имеет приоритет.
		 */
		int score(ImportLine item)
		{
			switch (pattern)
			{
				case LAYOUT_MODULE_IMPORTS:
					return item.moduleImport ? LAYOUT_GENERIC_MATCH : LAYOUT_NO_MATCH
				case LAYOUT_STATIC_IMPORTS:
					return item.staticImport ? LAYOUT_GENERIC_MATCH : LAYOUT_NO_MATCH
				case LAYOUT_OTHER_IMPORTS:
					return !item.staticImport && !item.moduleImport ? LAYOUT_GENERIC_MATCH : LAYOUT_NO_MATCH
				default:
					return scorePackage(item)
			}
		}

		private int scorePackage(ImportLine item)
		{
			var staticRule = pattern.startsWith(LAYOUT_STATIC_PREFIX)

			if (item.staticImport != staticRule || item.moduleImport)
			{
				return LAYOUT_NO_MATCH
			}

			var packagePattern = staticRule ? pattern.substring(LAYOUT_STATIC_PREFIX.length()) : pattern
			var prefix = LAYOUT_WILDCARD_SUFFIX.matcher(packagePattern).replaceFirst('') + NAME_SEPARATOR

			return item.name.startsWith(prefix) ? prefix.length() : LAYOUT_NO_MATCH
		}
	}
}
