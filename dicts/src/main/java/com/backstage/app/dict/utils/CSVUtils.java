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

package com.backstage.app.dict.utils;

import com.backstage.app.exception.AppException;
import com.backstage.app.model.other.exception.ApiStatusCodeImpl;
import lombok.experimental.UtilityClass;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

@UtilityClass
public class CSVUtils
{
	private static final CSVFormat MULTI_VALUED_CELL_FORMAT = CSVFormat.Builder.create(CSVFormat.DEFAULT)
			.setDelimiter(',')
			.setEscape('\\')
			.setQuoteMode(QuoteMode.MINIMAL)
			.setQuote('"')
			.build();

	public String[] parseMultiValuedCell(String stringValue)
	{
		try
		{
			return MULTI_VALUED_CELL_FORMAT.parse(new StringReader(stringValue)).getRecords().get(0).values();
		}
		catch (IOException e)
		{
			throw new AppException(ApiStatusCodeImpl.UNKNOWN_ERROR, e);
		}
	}

	public String buildMultiValuedCell(Iterable<?> values)
	{
		try (var stringWriter = new StringWriter();
		     var writer = new CSVPrinter(stringWriter, MULTI_VALUED_CELL_FORMAT))
		{
			writer.printRecord(values);

			return stringWriter.toString().trim();
		}
		catch (IOException e)
		{
			throw new AppException(ApiStatusCodeImpl.UNKNOWN_ERROR, e);
		}
	}
}
