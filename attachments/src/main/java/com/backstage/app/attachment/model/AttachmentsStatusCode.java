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

package com.backstage.app.attachment.model;

import com.backstage.app.model.other.exception.AppStatusCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum AttachmentsStatusCode implements AppStatusCode
{
	ATTACHMENT_ADD_ERROR(1, "Невозможно сохранить вложение."),
	ATTACHMENT_DELETE_ERROR(2, "Невозможно удалить вложение."),
	ATTACHMENT_TYPE_NOT_SUPPORTED(3, "Данный тип вложения не поддерживается.", HttpStatus.BAD_REQUEST),
	ATTACHMENT_INVALID_CONTENT(4, "Содержимое вложения не соответствует типу.", HttpStatus.BAD_REQUEST),
	ATTACHMENT_NOT_FOUND(5, "Вложение не найдено.", HttpStatus.NOT_FOUND),
	ATTACHMENT_DATA_NOT_AVAILABLE(6, "Данные вложения не доступны.", HttpStatus.NOT_FOUND),
	ATTACHMENT_STORE_INIT_FAILED(7, "Ошибка при инициализации хранилища вложений."),
	ATTACHMENT_STORE_ERROR(8, "При обращении к хранилищу вложений произошла ошибка."),
	ATTACHMENT_STORE_SYNC_ERROR(9, "Ошибка при синхронизации вложений между хранилищами.");

	private final Integer range = AppStatusCode.MODULE_RANGE_ATTACHMENTS;

	private final Integer code;

	private final String message;

	private final HttpStatusCode httpStatusCode;

	AttachmentsStatusCode(Integer code, String message)
	{
		this(code, message, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	AttachmentsStatusCode(Integer code, String message, HttpStatusCode httpStatusCode)
	{
		this.code = code;
		this.message = message.isEmpty() ? this.toString() : message;
		this.httpStatusCode = httpStatusCode;
	}
}
