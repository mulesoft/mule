/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.request.grizzly;

import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.http.api.HttpAuthentication;
import org.mule.module.http.internal.domain.ByteArrayHttpEntity;
import org.mule.module.http.internal.domain.InputStreamHttpEntity;
import org.mule.module.http.internal.domain.MultipartHttpEntity;
import org.mule.module.http.internal.domain.request.DefaultHttpRequest;
import org.mule.module.http.internal.domain.request.HttpRequest;
import org.mule.module.http.internal.domain.response.HttpResponse;
import org.mule.module.http.internal.domain.response.HttpResponseBuilder;
import org.mule.module.http.internal.request.DefaultHttpAuthentication;
import org.mule.module.http.internal.request.HttpAuthenticationType;
import org.mule.module.http.internal.request.HttpClient;
import org.mule.module.http.internal.request.NtlmProxyConfig;
import org.mule.module.http.internal.request.ProxyConfig;
import org.mule.transport.ssl.TlsContextFactory;
import org.mule.transport.tcp.TcpClientSocketProperties;
import org.mule.util.StringUtils;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.ProxyServer;
import com.ning.http.client.Realm;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.Response;
import com.ning.http.client.SSLEngineFactory;
import com.ning.http.client.generators.InputStreamBodyGenerator;
import com.ning.http.client.providers.grizzly.GrizzlyAsyncHttpProvider;
import com.ning.http.client.providers.grizzly.GrizzlyAsyncHttpProviderConfig;
import com.ning.http.client.providers.grizzly.TransportCustomizer;

import java.security.GeneralSecurityException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.servlet.http.Part;

public class GrizzlyHttpClient implements HttpClient
{

    private static final int MAX_CONNECTION_LIFETIME = 30 * 60 * 1000;

    private final TlsContextFactory tlsContextFactory;
    private final ProxyConfig proxyConfig;
    private final TcpClientSocketProperties clientSocketProperties;

    private int maxConcurrentConnections;
    private boolean usePersistentConnections;
    private int connectionIdleTimeout;

    private AsyncHttpClient asyncHttpClient;
    private SSLContext sslContext;

    public GrizzlyHttpClient(TlsContextFactory tlsContextFactory, ProxyConfig proxyConfig, TcpClientSocketProperties clientSocketProperties, int maxConcurrentConnections, boolean usePersistentConnections, int connectionIdleTimeout)
    {
        this.tlsContextFactory = tlsContextFactory;
        this.proxyConfig = proxyConfig;
        this.clientSocketProperties = clientSocketProperties;
        this.maxConcurrentConnections = maxConcurrentConnections;
        this.usePersistentConnections = usePersistentConnections;
        this.connectionIdleTimeout = connectionIdleTimeout;
    }

    @Override
    public void initialise() throws InitialisationException
    {
        AsyncHttpClientConfig.Builder builder = new AsyncHttpClientConfig.Builder();
        builder.setAllowPoolingConnection(true);
        builder.setFollowRedirects(true);

        configureTlsContext(builder);

        configureProxy(builder);

        configureClientSocketProperties(builder);

        configureConnections(builder);

        AsyncHttpClientConfig config = builder.build();

        asyncHttpClient = new AsyncHttpClient(new GrizzlyAsyncHttpProvider(config), config);
    }

    private void configureTlsContext(AsyncHttpClientConfig.Builder builder) throws InitialisationException
    {
        if (tlsContextFactory != null)
        {
            try
            {
                sslContext = tlsContextFactory.createSslContext();
            }
            catch (Exception e)
            {
                throw new InitialisationException(CoreMessages.createStaticMessage("Cannot initialize SSL context"), e, this);
            }

            builder.setSSLEngineFactory(new SSLEngineFactory()
            {
                @Override
                public SSLEngine newSSLEngine() throws GeneralSecurityException
                {
                    SSLEngine sslEngine = sslContext.createSSLEngine();

                    sslEngine.setEnabledCipherSuites(tlsContextFactory.getEnabledCipherSuites());
                    sslEngine.setEnabledProtocols(tlsContextFactory.getEnabledProtocols());

                    return sslEngine;
                }
            });
        }
    }

    private void configureProxy(AsyncHttpClientConfig.Builder builder)
    {
        if (proxyConfig != null)
        {
            ProxyServer proxyServer;
            if (!StringUtils.isEmpty(proxyConfig.getUsername()))
            {
                proxyServer = new ProxyServer(
                        proxyConfig.getHost(),
                        Integer.parseInt(proxyConfig.getPort()),
                        proxyConfig.getUsername(),
                        proxyConfig.getPassword());
                if (proxyConfig instanceof NtlmProxyConfig)
                {
                    proxyServer.setNtlmDomain(((NtlmProxyConfig) proxyConfig).getNtlmDomain());
                }
            }
            else
            {
                proxyServer = new ProxyServer(
                        proxyConfig.getHost(),
                        Integer.parseInt(proxyConfig.getPort()));
            }
            builder.setProxyServer(proxyServer);
        }
    }
    private void configureClientSocketProperties(AsyncHttpClientConfig.Builder builder)
    {
        if (clientSocketProperties != null)
        {
            GrizzlyAsyncHttpProviderConfig providerConfig = new GrizzlyAsyncHttpProviderConfig();
            TransportCustomizer customizer = new SocketConfigTransportCustomizer(clientSocketProperties);
            providerConfig.addProperty(GrizzlyAsyncHttpProviderConfig.Property.TRANSPORT_CUSTOMIZER, customizer);
            builder.setAsyncHttpClientProviderConfig(providerConfig);
        }
    }

    private void configureConnections(AsyncHttpClientConfig.Builder builder) throws InitialisationException
    {
        builder.setMaximumConnectionsTotal(maxConcurrentConnections);
        builder.setMaximumConnectionsPerHost(maxConcurrentConnections);

        builder.setAllowPoolingConnection(usePersistentConnections);
        builder.setAllowSslConnectionPool(usePersistentConnections);

        builder.setMaxConnectionLifeTimeInMs(MAX_CONNECTION_LIFETIME);
        builder.setIdleConnectionInPoolTimeoutInMs(connectionIdleTimeout);
    }

    @Override
    public HttpResponse send(HttpRequest request, int responseTimeout, boolean followRedirects, HttpAuthentication authentication) throws Exception
    {

        RequestBuilder builder = new RequestBuilder();

        builder.setMethod(request.getMethod());
        builder.setUrl(request.getUri());
        builder.setFollowRedirects(followRedirects);

        for (String headerName : request.getHeaderNames())
        {
            for (String headerValue : request.getHeaderValues(headerName))
            {
                builder.addHeader(headerName, headerValue);
            }
        }

        DefaultHttpRequest defaultHttpRequest = (DefaultHttpRequest) request;

        for (String queryParamName : defaultHttpRequest.getQueryParams().keySet())
        {
            for (String queryParamValue : defaultHttpRequest.getQueryParams().getAsList(queryParamName))
            {
                builder.addQueryParameter(queryParamName, queryParamValue);
            }
        }

        if (authentication != null && authentication instanceof DefaultHttpAuthentication)
        {
            DefaultHttpAuthentication defaultHttpAuthentication = (DefaultHttpAuthentication)authentication;

            Realm.RealmBuilder realmBuilder = new Realm.RealmBuilder()
                        .setPrincipal(defaultHttpAuthentication.getUsername())
                        .setPassword(defaultHttpAuthentication.getPassword())
                        .setUsePreemptiveAuth(false);

            if (defaultHttpAuthentication.getType() == HttpAuthenticationType.BASIC)
            {
                realmBuilder.setScheme(Realm.AuthScheme.BASIC);
            }
            else if (defaultHttpAuthentication.getType() == HttpAuthenticationType.DIGEST)
            {
                realmBuilder.setScheme(Realm.AuthScheme.DIGEST);
            }

            builder.setRealm(realmBuilder.build());

        }

        if (request.getEntity() != null)
        {
            if (request.getEntity() instanceof InputStreamHttpEntity)
            {
                builder.setBody(new InputStreamBodyGenerator(((InputStreamHttpEntity) request.getEntity()).getInputStream()));
            }
            else if (request.getEntity() instanceof ByteArrayHttpEntity)
            {
                builder.setBody(((ByteArrayHttpEntity) request.getEntity()).getContent());
            }
            else if (request.getEntity() instanceof MultipartHttpEntity)
            {
                MultipartHttpEntity multipartHttpEntity = (MultipartHttpEntity) request.getEntity();

                for (Part part : multipartHttpEntity.getParts())
                {
                    builder.addBodyPart(new PartWrapper(part));
                }
            }
        }

        ListenableFuture<Response> future = asyncHttpClient.executeRequest(builder.build());
        Response response = future.get(responseTimeout, TimeUnit.MILLISECONDS);

        HttpResponseBuilder responseBuilder = new HttpResponseBuilder();
        responseBuilder.setStatusCode(response.getStatusCode());
        responseBuilder.setEntity(new InputStreamHttpEntity(response.getResponseBodyAsStream()));

        for (String header : response.getHeaders().keySet())
        {
            for (String headerValue : response.getHeaders(header))
            {
                responseBuilder.addHeader(header, headerValue);
            }
        }

        return responseBuilder.build();

    }

    @Override
    public void stop()
    {
        asyncHttpClient.close();
    }
}
