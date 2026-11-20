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

package com.backstage.app;

import com.backstage.app.utils.RestUtils;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RestUtilsTest
{
	@Test
	public void testCorrectRequest() throws IOException
	{
		var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/", exchange -> {
			try (exchange)
			{
				var userAgent = String.valueOf(exchange.getRequestHeaders().getFirst("User-Agent"));
				var body = userAgent.getBytes(StandardCharsets.UTF_8);

				exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
				exchange.sendResponseHeaders(200, body.length);
				exchange.getResponseBody().write(body);
			}
		});
		server.start();

		try
		{
			var restClient = RestUtils.createRestClient(5, 2, 1,
					(builder, connectionManager) -> builder.setUserAgent("RestUtilsTest"));
			var response = restClient.get()
					.uri("http://127.0.0.1:%s/".formatted(server.getAddress().getPort()))
					.retrieve()
					.toEntity(String.class);

			assertEquals(HttpStatus.OK, response.getStatusCode());
			assertEquals("RestUtilsTest", response.getBody());
		}
		finally
		{
			server.stop(0);
		}
	}
}
