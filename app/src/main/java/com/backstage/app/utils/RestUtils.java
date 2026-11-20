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

package com.backstage.app.utils;

import lombok.experimental.UtilityClass;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.client5.http.ssl.HostnameVerificationPolicy;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
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

	public RestClient createTrustfulRestClient() throws Exception
	{
		return createTrustfulRestClient(DEFAULT_REQUEST_TIMEOUT_SECONDS);
	}

	public RestClient createTrustfulRestClient(int requestTimeoutSeconds) throws Exception
	{
		return createTrustfulRestClient(requestTimeoutSeconds, null);
	}

	public RestClient createTrustfulRestClient(int requestTimeoutSeconds, int maxConnectionPoolSize, int maxConnectionPerRoute) throws Exception
	{
		return createRestClient(requestTimeoutSeconds, maxConnectionPoolSize, maxConnectionPerRoute, withSslConfigurator(null));
	}

	public RestClient createTrustfulRestClient(int requestTimeoutSeconds, BiConsumer<HttpClientBuilder, PoolingHttpClientConnectionManagerBuilder> httpClientConfigurator) throws Exception
	{
		return createRestClient(requestTimeoutSeconds, withSslConfigurator(httpClientConfigurator));
	}

	public RestClient createRestClient(int requestTimeoutSeconds)
	{
		return createRestClient(requestTimeoutSeconds, null);
	}

	public RestClient createRestClient(int requestTimeoutSeconds, BiConsumer<HttpClientBuilder, PoolingHttpClientConnectionManagerBuilder> httpClientConfigurator)
	{
		return createRestClient(requestTimeoutSeconds, 0, 0, httpClientConfigurator);
	}

	public RestClient createRestClient(int requestTimeoutSeconds, int maxConnectionPoolSize, int maxConnectionPerRoute,
			BiConsumer<HttpClientBuilder, PoolingHttpClientConnectionManagerBuilder> httpClientConfigurator)
	{
		return RestClient.builder()
				.requestFactory(createRequestFactory(requestTimeoutSeconds, maxConnectionPoolSize, maxConnectionPerRoute, httpClientConfigurator))
				.build();
	}

	@Deprecated
	@SuppressWarnings("removal")
	public RestTemplate createTrustfulRestTemplate() throws Exception
	{
		return createTrustfulRestTemplate(DEFAULT_REQUEST_TIMEOUT_SECONDS);
	}

	@Deprecated
	@SuppressWarnings("removal")
	public RestTemplate createTrustfulRestTemplate(int requestTimeoutSeconds) throws Exception
	{
		return createTrustfulRestTemplate(requestTimeoutSeconds, null);
	}

	@Deprecated
	@SuppressWarnings("removal")
	public RestTemplate createTrustfulRestTemplate(int requestTimeoutSeconds, int maxConnectionPoolSize, int maxConnectionPerRoute) throws Exception
	{
		return createRestTemplate(requestTimeoutSeconds, maxConnectionPoolSize, maxConnectionPerRoute, withSslConfigurator(null));
	}

	@Deprecated
	@SuppressWarnings("removal")
	public RestTemplate createTrustfulRestTemplate(int requestTimeoutSeconds, BiConsumer<HttpClientBuilder, PoolingHttpClientConnectionManagerBuilder> httpClientConfigurator) throws Exception
	{
		return createRestTemplate(requestTimeoutSeconds, withSslConfigurator(httpClientConfigurator));
	}

	@Deprecated
	@SuppressWarnings("removal")
	public RestTemplate createRestTemplate(int requestTimeoutSeconds)
	{
		return createRestTemplate(requestTimeoutSeconds, null);
	}

	@Deprecated
	@SuppressWarnings("removal")
	public RestTemplate createRestTemplate(int requestTimeoutSeconds, BiConsumer<HttpClientBuilder, PoolingHttpClientConnectionManagerBuilder> httpClientConfigurator)
	{
		return createRestTemplate(requestTimeoutSeconds, 0, 0, httpClientConfigurator);
	}

	@Deprecated
	@SuppressWarnings("removal")
	public RestTemplate createRestTemplate(int requestTimeoutSeconds, int maxConnectionPoolSize, int maxConnectionPerRoute, BiConsumer<HttpClientBuilder, PoolingHttpClientConnectionManagerBuilder> httpClientConfigurator)
	{
		return new RestTemplate(createRequestFactory(requestTimeoutSeconds, maxConnectionPoolSize, maxConnectionPerRoute, httpClientConfigurator));
	}

	private HttpComponentsClientHttpRequestFactory createRequestFactory(int requestTimeoutSeconds, int maxConnectionPoolSize, int maxConnectionPerRoute,
			BiConsumer<HttpClientBuilder, PoolingHttpClientConnectionManagerBuilder> httpClientConfigurator)
	{
		var connectionConfig = ConnectionConfig.custom()
				.setConnectTimeout(requestTimeoutSeconds, TimeUnit.SECONDS)
				.build();

		PoolingHttpClientConnectionManagerBuilder connectionManagerBuilder = PoolingHttpClientConnectionManagerBuilder
				.create()
				.setMaxConnTotal(maxConnectionPoolSize)
				.setMaxConnPerRoute(maxConnectionPerRoute)
				.setDefaultConnectionConfig(connectionConfig);

		var httpClientBuilder = HttpClients.custom()
				.setDefaultRequestConfig(getRequestConfig(requestTimeoutSeconds));

		if (httpClientConfigurator != null)
		{
			httpClientConfigurator.accept(httpClientBuilder, connectionManagerBuilder);
		}

		httpClientBuilder.setConnectionManager(connectionManagerBuilder.build());

		return new HttpComponentsClientHttpRequestFactory(httpClientBuilder.build());
	}

	private BiConsumer<HttpClientBuilder, PoolingHttpClientConnectionManagerBuilder> withSslConfigurator(
			BiConsumer<HttpClientBuilder, PoolingHttpClientConnectionManagerBuilder> httpClientConfigurator) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException
	{
		var sslContextBuilder = new SSLContextBuilder();
		sslContextBuilder.loadTrustMaterial(null, TrustAllStrategy.INSTANCE);

		var tlsStrategy = ClientTlsStrategyBuilder.create()
				.setSslContext(sslContextBuilder.build())
				.setHostVerificationPolicy(HostnameVerificationPolicy.CLIENT)
				.setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
				.buildClassic();

		BiConsumer<HttpClientBuilder, PoolingHttpClientConnectionManagerBuilder> sslConfigurator = (builder, connectionManager) -> connectionManager.setTlsSocketStrategy(tlsStrategy);

		return httpClientConfigurator == null ? sslConfigurator : httpClientConfigurator.andThen(sslConfigurator);
	}

	private RequestConfig getRequestConfig(int requestTimeoutSeconds)
	{
		return RequestConfig.custom()
				.setConnectionRequestTimeout(requestTimeoutSeconds, TimeUnit.SECONDS)
				.setResponseTimeout(requestTimeoutSeconds, TimeUnit.SECONDS)
				.build();
	}
}
