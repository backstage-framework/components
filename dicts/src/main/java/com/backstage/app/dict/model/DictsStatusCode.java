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

package com.backstage.app.dict.model;

import com.backstage.app.model.other.exception.AppStatusCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum DictsStatusCode implements AppStatusCode
{
	MIGRATION_APPLIED_ERROR(1, "Ошибка применения миграции."),
	MIGRATION_FILE_READ_ERROR(2, "Ошибка чтения миграции из файла."),
	MIGRATIONS_HAS_SAME_VERSION(3, "Миграция имеет одинаковую версию."),
	MIGRATION_PROCESS_UNKNOWN_ERROR(4, "Неизвестная ошибка при обработки миграций."),

	SQL_PARSE_SYNTAX_ERROR(5, "Синтаксическая ошибка парсинга SQL выражения.", HttpStatus.BAD_REQUEST),

	PREPARE_PAGEABLE_MONGO_ERROR(6, "Ошибка при адаптации pageable к MongoDB адаптеру."),

	ENGINE_ERROR(7, "Ошибка при обработке engine."),
	STORAGE_ERROR(8, "Ошибка при обработке storage."),

	DICTS_ERROR(9, "При обращении к справочникам произошла ошибка.");

	private final Integer range = AppStatusCode.MODULE_RANGE_DICTS;

	private final Integer code;

	private final String message;

	private final HttpStatusCode httpStatusCode;

	DictsStatusCode(Integer code, String message)
	{
		this(code, message, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	DictsStatusCode(Integer code, String message, HttpStatusCode httpStatusCode)
	{
		this.code = code;
		this.message = message.isEmpty() ? this.toString() : message;
		this.httpStatusCode = httpStatusCode;
	}
}
