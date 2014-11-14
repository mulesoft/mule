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
import org.mule.module.http.internal.request.NtlmProxyConfig;
import org.mule.module.http.internal.request.ProxyConfig;
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

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.servlet.http.Part;

public class GrizzlyHttpClient implements HttpClient
{
    private final TlsContextFactory tlsContextFactory;
    private final ProxyConfig proxyConfig;
    private final TcpClientSocketProperties clientSocketProperties;

    private AsyncHttpClient asyncHttpClient;
    private SSLContext sslContext;

    public GrizzlyHttpClient(TlsContextFactory tlsContextFactory, ProxyConfig proxyConfig, TcpClientSocketProperties clientSocketProperties)
    {
        this.tlsContextFactory = tlsContextFactory;
        this.proxyConfig = proxyConfig;
        this.clientSocketProperties = clientSocketProperties;
    }

    @Override
    public void initialise() throws InitialisationException
    {
        AsyncHttpClientConfig.Builder builder = new AsyncHttpClientConfig.Builder();
        builder.setAllowPoolingConnection(true);

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

        if (clientSocketProperties != null)
        {
            GrizzlyAsyncHttpProviderConfig providerConfig = new GrizzlyAsyncHttpProviderConfig();
            TransportCustomizer customizer = new SocketConfigTransportCustomizer(clientSocketProperties);
            providerConfig.addProperty(GrizzlyAsyncHttpProviderConfig.Property.TRANSPORT_CUSTOMIZER, customizer);
            builder.setAsyncHttpClientProviderConfig(providerConfig);
        }

        AsyncHttpClientConfig config = builder.build();

        asyncHttpClient = new AsyncHttpClient(new GrizzlyAsyncHttpProvider(config), config);

    }

    @Override
    public HttpResponse send(HttpRequest request, int responseTimeout, boolean followRedirects, HttpAuthentication authentication) throws IOException, TimeoutException
    {

        RequestBuilder builder = new RequestBuilder();

        builder.setMethod(request.getMethod());
        builder.setUrl(request.getUri().toString());
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
        Response response = null;

        try
        {
            response = future.get(responseTimeout, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException e)
        {
            throw new IOException(e);
        }
        catch (ExecutionException e)
        {
            throw new IOException(e);
        }

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
