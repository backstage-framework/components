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

package com.backstage.app.model.other.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum AppStatusCodeImpl implements AppStatusCode
{
	OK(0, "Операция выполнена успешно.", HttpStatus.OK),
	UNKNOWN_ERROR(1, "Неизвестная ошибка."),
	ACCESS_RIGHTS_ERROR(2, "Ошибка прав доступа.", HttpStatus.FORBIDDEN),
	ILLEGAL_INPUT(3, "Некорректные входные данные.", HttpStatus.BAD_REQUEST),
	ILLEGAL_DATA_FORMAT(4, "Некорректный формат данных.", HttpStatus.BAD_REQUEST),
	OBJECT_NOT_FOUND(5, "Указанный объект не найден.", HttpStatus.NOT_FOUND),
	CAPTCHA_CHECK_ERROR(6, "Ошибка проверки капчи."),

	SERIALIZE_ERROR(30, "Ошибка сериализации обьекта."),
	DESERIALIZE_ERROR(31, "Ошибка десериализации обьекта."),

	// TODO: потеряли логику присвоения кодов ниже.
	REPORT_GENERATE_ERROR(400, "При генерации отчета произошла ошибка."),

	DATE_PARSE_ERROR(500, "Неправильный формат даты."),

	REMOTE_SERVICE_ERROR(600, "При обращении к сервису произошла ошибка.");

	private final Integer range = AppStatusCode.MODULE_RANGE_APP;

	private final Integer code;

	private final String message;

	private final HttpStatusCode httpStatusCode;

	AppStatusCodeImpl(Integer code, String message)
	{
		this(code, message, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	AppStatusCodeImpl(Integer code, String message, HttpStatusCode httpStatusCode)
	{
		this.code = code;
		this.message = message.isEmpty() ? this.toString() : message;
		this.httpStatusCode = httpStatusCode;
	}
}
