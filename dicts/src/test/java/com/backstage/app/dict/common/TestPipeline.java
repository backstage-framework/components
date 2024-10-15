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

package com.backstage.app.dict.common;

public class TestPipeline
{
	public static final int FIRST_INIT = 10;

	public static final int POSTGRES_STORAGE_MIGRATION = 20;
	public static final int POSTGRES_DICT = 30;
	public static final int POSTGRES_DICT_DATA = 40;
	public static final int POSTGRES_DICT_DATA_CONVERSION = 50;
	public static final int POSTGRES_CLASSPATH_MIGRATION = 60;
	public static final int POSTGRES_DICT_VALIDATION = 70;
	public static final int POSTGRES_DICT_DATA_VALIDATION = 80;
	public static final int POSTGRES_MAPPING = 90;
	public static final int POSTGRES_EXPORT = 100;
	public static final int POSTGRES_INTERPRETER = 110;
	public static final int POSTGRES_CSV_IMPORT = 120;

	public static final int MONGO_STORAGE_MIGRATION = 130;
	public static final int MONGO_DICT = 140;
	public static final int MONGO_DICT_DATA = 150;
	public static final int MONGO_DICT_DATA_CONVERSION = 160;
	public static final int MONGO_CLASSPATH_MIGRATION = 170;
	public static final int MONGO_DICT_VALIDATION = 180;
	public static final int MONGO_DICT_DATA_VALIDATION = 190;
	public static final int MONGO_MAPPING = 200;
	public static final int MONGO_EXPORT = 210;
	public static final int MONGO_INTERPRETER = 220;
	public static final int MONGO_CSV_IMPORT = 230;

	public static final int DICT_GET_ALL_TEST = -100;
	public static final int DICT_GET_BY_ID_TEST = -200;
	public static final int DICT_DATA_GET_BY_FILTER_TEST = -200;
	public static final int DICT_DATA_GET_BY_FILTER_WITH_LOGICAL_EXPRESSION_TEST = -150;
	public static final int DICT_DATA_EXISTS_BY_FILTER_TEST = -100;
	public static final int DICT_DATA_GET_DISTINCT_VALUES_BY_FILTER_TEST = -50;
}
