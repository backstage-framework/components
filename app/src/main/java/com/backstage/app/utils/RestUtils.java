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

package com.backstage.app.utils;

import lombok.experimental.UtilityClass;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

@UtilityClass
public class RestUtils
{
	private static final int DEFAULT_REQUEST_TIMEOUT_SECONDS = 60;

	public RestTemplate createTrustfulRestTemplate() throws Exception
	{
		return createTrustfulRestTemplate(DEFAULT_REQUEST_TIMEOUT_SECONDS);
	}

	public RestTemplate createTrustfulRestTemplate(int requestTimeoutSeconds) throws Exception
	{
		return createTrustfulRestTemplate(requestTimeoutSeconds, null);
	}

	public RestTemplate createTrustfulRestTemplate(int requestTimeoutSeconds, int maxConnectionPoolSize, int maxConnectionPerRoute) throws Exception
	{
		return createRestTemplate(requestTimeoutSeconds, maxConnectionPoolSize, maxConnectionPerRoute, getSslConfigurator());
	}

	public RestTemplate createTrustfulRestTemplate(int requestTimeoutSeconds, BiConsumer<HttpClientBuilder, PoolingHttpClientConnectionManagerBuilder> httpClientConfigurator) throws Exception
	{
		var sslConfigurator = getSslConfigurator();

		return createRestTemplate(requestTimeoutSeconds, httpClientConfigurator == null ? sslConfigurator : httpClientConfigurator.andThen(sslConfigurator));
	}

	public RestTemplate createRestTemplate(int requestTimeoutSeconds)
	{
		return createRestTemplate(requestTimeoutSeconds, null);
	}

	public RestTemplate createRestTemplate(int requestTimeoutSeconds, BiConsumer<HttpClientBuilder, PoolingHttpClientConnectionManagerBuilder> httpClientConfigurator)
	{
		return createRestTemplate(requestTimeoutSeconds, 0, 0, httpClientConfigurator);
	}

	public RestTemplate createRestTemplate(int requestTimeoutSeconds, int maxConnectionPoolSize, int maxConnectionPerRoute, BiConsumer<HttpClientBuilder, PoolingHttpClientConnectionManagerBuilder> httpClientConfigurator)
	{
		PoolingHttpClientConnectionManagerBuilder connectionManagerBuilder = PoolingHttpClientConnectionManagerBuilder
				.create()
				.setMaxConnTotal(maxConnectionPoolSize)
				.setMaxConnPerRoute(maxConnectionPerRoute);

		var httpClientBuilder = HttpClients.custom()
				.setDefaultRequestConfig(getRequestConfig(requestTimeoutSeconds));

		if (httpClientConfigurator != null)
		{
			httpClientConfigurator.accept(httpClientBuilder, connectionManagerBuilder);
		}

		httpClientBuilder.setConnectionManager(connectionManagerBuilder.build());

		var httpClient = httpClientBuilder.build();
		var httpRequestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);

		var restTemplate = new RestTemplate();
		restTemplate.setRequestFactory(httpRequestFactory);

		return restTemplate;
	}

	private BiConsumer<HttpClientBuilder, PoolingHttpClientConnectionManagerBuilder> getSslConfigurator() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException
	{
		var sslContextBuilder = new SSLContextBuilder();
		sslContextBuilder.loadTrustMaterial(null, new TrustAllStrategy());

		var socketFactory = new SSLConnectionSocketFactory(sslContextBuilder.build(), new NoopHostnameVerifier());

		return (builder, connectionManager) -> connectionManager.setSSLSocketFactory(socketFactory);
	}

	private RequestConfig getRequestConfig(int requestTimeoutSeconds)
	{
		return RequestConfig.custom()
				.setConnectionRequestTimeout(requestTimeoutSeconds, TimeUnit.SECONDS)
				.setConnectTimeout(requestTimeoutSeconds, TimeUnit.SECONDS)
				.setResponseTimeout(requestTimeoutSeconds, TimeUnit.SECONDS)
				.build();
	}
}
