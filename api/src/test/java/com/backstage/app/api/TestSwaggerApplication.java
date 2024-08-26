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

package com.backstage.app.api;

import com.backstage.app.api.configuration.openapi.MultipleOpenApiSupportConfiguration;
import com.backstage.app.api.configuration.openapi.SpringDocConfigProperties;
import com.backstage.app.api.configuration.openapi.SpringDocConfiguration;
import com.backstage.app.api.configuration.openapi.SpringDocWebMvcConfiguration;
import com.backstage.app.api.configuration.properties.ApiProperties;
import com.backstage.app.configuration.properties.AppProperties;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Import;

@Import({ApiProperties.class, AppProperties.class, MultipleOpenApiSupportConfiguration.class, SpringDocConfiguration.class, SpringDocConfigProperties.class, SpringDocWebMvcConfiguration.class})
@SpringBootConfiguration
@ImportAutoConfiguration(exclude = SecurityAutoConfiguration.class)
public class TestSwaggerApplication
{
}
