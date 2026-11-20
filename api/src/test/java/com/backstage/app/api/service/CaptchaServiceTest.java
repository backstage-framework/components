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

package com.backstage.app.api.service;

import com.backstage.app.api.configuration.properties.CaptchaProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CaptchaServiceTest
{
	@Test
	void checksCaptchaWithEncodedQueryParameters() throws IOException
	{
		var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/verify", exchange -> {
			try (exchange)
			{
				var expectedQuery = "mode=test&secret=first%26second&response=token%20with%3Dvalue";
				var success = "GET".equals(exchange.getRequestMethod()) && expectedQuery.equals(exchange.getRequestURI().getRawQuery());
				var body = "{\"success\":%s,\"score\":0.9}".formatted(success).getBytes(StandardCharsets.UTF_8);

				exchange.getResponseHeaders().set("Content-Type", "application/json");
				exchange.sendResponseHeaders(200, body.length);
				exchange.getResponseBody().write(body);
			}
		});
		server.start();

		try
		{
			var properties = new CaptchaProperties();
			properties.setApi("http://127.0.0.1:%s/verify?mode=test".formatted(server.getAddress().getPort()));
			properties.setSecret("first&second");

			assertTrue(new CaptchaService(properties).check("token with=value"));
		}
		finally
		{
			server.stop(0);
		}
	}
}
