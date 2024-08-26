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

package com.backstage.app.report.model;

import com.backstage.app.report.model.filter.ReportFilter;
import com.backstage.app.report.model.filter.SimpleReportFilter;
import com.backstage.app.report.service.generator.ExampleGenerator;
import com.backstage.app.report.service.generator.ReportGenerator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExampleReportType implements ReportType
{
	EXAMPLE_1("Пример отчета 1", ReportFileType.XLSX, ExampleGenerator.class, SimpleReportFilter.class);

	private final String title;
	private final ReportFileType reportFileType;
	private final Class<? extends ReportGenerator<? extends ReportFilter>> generatorType;
	private final Class<? extends ReportFilter> filterType;
}
