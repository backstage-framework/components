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

package com.backstage.bpm.configuration.plugin.listener;

import com.backstage.bpm.model.EngineType;
import com.backstage.bpm.service.script.extension.ScriptingExtension;
import com.backstage.bpm.service.script.extension.ScriptingExtensionLocator;
import org.camunda.bpm.engine.delegate.VariableScope;
import org.camunda.bpm.engine.impl.scripting.engine.Resolver;
import org.camunda.bpm.engine.impl.scripting.engine.ResolverFactory;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ScriptingExtensionResolver implements ResolverFactory, Resolver
{
	private final Map<String, ScriptingExtension> scriptingExtensions;

	public ScriptingExtensionResolver(ScriptingExtensionLocator scriptingExtensionLocator)
	{
		this.scriptingExtensions = scriptingExtensionLocator.getScriptingExtensions(EngineType.CAMUNDA).stream().collect(Collectors.toMap(ScriptingExtension::getBindingName, Function.identity()));
	}

	@Override
	public boolean containsKey(Object key)
	{
		return scriptingExtensions.containsKey(key);
	}

	@Override
	public Object get(Object key)
	{
		return scriptingExtensions.get(key);
	}

	@Override
	public Set<String> keySet()
	{
		return scriptingExtensions.keySet();
	}

	@Override
	public Resolver createResolver(VariableScope variableScope)
	{
		return this;
	}
}
