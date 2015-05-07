/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.request.grizzly;

import org.mule.api.CompletionHandler;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.http.api.requester.proxy.ProxyConfig;
import org.mule.module.http.internal.domain.ByteArrayHttpEntity;
import org.mule.module.http.internal.domain.InputStreamHttpEntity;
import org.mule.module.http.internal.domain.MultipartHttpEntity;
import org.mule.module.http.internal.domain.request.DefaultHttpRequest;
import org.mule.module.http.internal.domain.request.HttpRequest;
import org.mule.module.http.internal.domain.request.HttpRequestAuthentication;
import org.mule.module.http.internal.domain.response.HttpResponse;
import org.mule.module.http.internal.domain.response.HttpResponseBuilder;
import org.mule.module.http.internal.multipart.HttpPart;
import org.mule.module.http.internal.request.HttpAuthenticationType;
import org.mule.module.http.internal.request.HttpClient;
import org.mule.module.http.internal.request.NtlmProxyConfig;
import org.mule.transport.ssl.api.TlsContextFactory;
import org.mule.transport.tcp.TcpClientSocketProperties;
import org.mule.util.IOUtils;
import org.mule.util.StringUtils;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.ProxyServer;
import com.ning.http.client.Realm;
import com.ning.http.client.Request;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.Response;
import com.ning.http.client.generators.InputStreamBodyGenerator;
import com.ning.http.client.multipart.ByteArrayPart;
import com.ning.http.client.providers.grizzly.GrizzlyAsyncHttpProvider;
import com.ning.http.client.providers.grizzly.GrizzlyAsyncHttpProviderConfig;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.SSLContext;

public class GrizzlyHttpClient implements HttpClient
{

    private static final int MAX_CONNECTION_LIFETIME = 30 * 60 * 1000;

    private final TlsContextFactory tlsContextFactory;
    private final ProxyConfig proxyConfig;
    private final TcpClientSocketProperties clientSocketProperties;

    private int maxConnections;
    private boolean usePersistentConnections;
    private int connectionIdleTimeout;
    private String threadNamePrefix;

    private AsyncHttpClient asyncHttpClient;
    private SSLContext sslContext;

    public GrizzlyHttpClient(GrizzlyHttpClientConfiguration config)
    {
        this.tlsContextFactory = config.getTlsContextFactory();
        this.proxyConfig = config.getProxyConfig();
        this.clientSocketProperties = config.getClientSocketProperties();
        this.maxConnections = config.getMaxConnections();
        this.usePersistentConnections = config.isUsePersistentConnections();
        this.connectionIdleTimeout = config.getConnectionIdleTimeout();
        this.threadNamePrefix = config.getThreadNamePrefix();
    }

    @Override
    public void initialise() throws InitialisationException
    {
        AsyncHttpClientConfig.Builder builder = new AsyncHttpClientConfig.Builder();
        builder.setAllowPoolingConnections(true);

        configureTransport(builder);

        configureTlsContext(builder);

        configureProxy(builder);

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

            // This sets all the TLS configuration needed, except for the enabled protocols and cipher suites.
            builder.setSSLContext(sslContext);
            //These complete the set up
            if (tlsContextFactory.getEnabledCipherSuites() != null)
            {
                builder.setEnabledCipherSuites(tlsContextFactory.getEnabledCipherSuites());
            }
            if (tlsContextFactory.getEnabledProtocols() != null)
            {
                builder.setEnabledProtocols(tlsContextFactory.getEnabledProtocols());
            }

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
                        proxyConfig.getPort(),
                        proxyConfig.getUsername(),
                        proxyConfig.getPassword());
                if (proxyConfig instanceof NtlmProxyConfig)
                {
                    proxyServer.setNtlmDomain(((NtlmProxyConfig) proxyConfig).getNtlmDomain());
                }
            }
            else
            {
                proxyServer = new ProxyServer(proxyConfig.getHost(),proxyConfig.getPort());
            }
            builder.setProxyServer(proxyServer);
        }
    }

    private void configureTransport(AsyncHttpClientConfig.Builder builder)
    {
        GrizzlyAsyncHttpProviderConfig providerConfig = new GrizzlyAsyncHttpProviderConfig();
        CompositeTransportCustomizer compositeTransportCustomizer = new CompositeTransportCustomizer();
        compositeTransportCustomizer.addTransportCustomizer(new SameThreadIOStrategyTransportCustomizer
                                                                    (threadNamePrefix));
        compositeTransportCustomizer.addTransportCustomizer(new LoggerTransportCustomizer());

        if (clientSocketProperties != null)
        {
            compositeTransportCustomizer.addTransportCustomizer(new SocketConfigTransportCustomizer(clientSocketProperties));
        }

        providerConfig.addProperty(GrizzlyAsyncHttpProviderConfig.Property.TRANSPORT_CUSTOMIZER, compositeTransportCustomizer);
        //Grizzly now decompresses encoded responses, this flag maintains the previous behaviour
        providerConfig.addProperty(GrizzlyAsyncHttpProviderConfig.Property.DECOMPRESS_RESPONSE, Boolean.FALSE);
        builder.setAsyncHttpClientProviderConfig(providerConfig);
    }

    private void configureConnections(AsyncHttpClientConfig.Builder builder) throws InitialisationException
    {
        if (maxConnections > 0)
        {
            builder.addRequestFilter(new CustomTimeoutThrottleRequestFilter(maxConnections));
        }

        builder.setMaxConnections(maxConnections);
        builder.setMaxConnectionsPerHost(maxConnections);

        builder.setAllowPoolingConnections(usePersistentConnections);
        builder.setAllowPoolingSslConnections(usePersistentConnections);

        builder.setConnectionTTL(MAX_CONNECTION_LIFETIME);
        builder.setPooledConnectionIdleTimeout(connectionIdleTimeout);

        builder.setIOThreadMultiplier(1);
    }

    @Override
    public HttpResponse send(HttpRequest request, int responseTimeout, boolean followRedirects, HttpRequestAuthentication authentication) throws IOException, TimeoutException
    {

        Request grizzlyRequest= createGrizzlyRequest(request, responseTimeout, followRedirects, authentication);
        ListenableFuture<Response> future = asyncHttpClient.executeRequest(grizzlyRequest);
        try
        {
            return createMuleResponse(future.get(responseTimeout, TimeUnit.MILLISECONDS));
        }
        catch (InterruptedException e)
        {
            throw new IOException(e);
        }
        catch (ExecutionException e)
        {
            throw new IOException(e);
        }
    }

    @Override
    public void send(HttpRequest request, int responseTimeout, boolean followRedirects, HttpRequestAuthentication
            authentication, final CompletionHandler<HttpResponse, Exception> completionHandler)
    {
        try
        {
            final ListenableFuture<Response> future = asyncHttpClient.executeRequest(createGrizzlyRequest(request,
                                                                                                          responseTimeout, followRedirects, authentication), new AsyncCompletionHandler<Response>()
            {
                @Override
                public Response onCompleted(Response response) throws Exception
                {
                    completionHandler.onCompletion(createMuleResponse(response));
                    return null;
                }

                @Override
                public void onThrowable(Throwable t)
                {
                    completionHandler.onFailure((Exception) t);
                }
            });
        }
        catch (Exception e)
        {
            completionHandler.onFailure(e);
        }
    }

    private HttpResponse createMuleResponse(Response response) throws IOException
    {
        HttpResponseBuilder responseBuilder = new HttpResponseBuilder();
        responseBuilder.setStatusCode(response.getStatusCode());
        responseBuilder.setReasonPhrase(response.getStatusText());
        responseBuilder.setEntity(new InputStreamHttpEntity(response.getResponseBodyAsStream()));

        if (response.hasResponseHeaders())
        {
            for (String header : response.getHeaders().keySet())
            {
                for (String headerValue : response.getHeaders(header))
                {
                    responseBuilder.addHeader(header, headerValue);
                }
            }
        }
        return responseBuilder.build();
    }

    private Request createGrizzlyRequest(HttpRequest request, int responseTimeout, boolean followRedirects,
                                         HttpRequestAuthentication authentication) throws IOException
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
            for (String queryParamValue : defaultHttpRequest.getQueryParams().getAll(queryParamName))
            {
                builder.addQueryParam(queryParamName, queryParamValue);
            }
        }

        if (authentication != null)
        {
            Realm.RealmBuilder realmBuilder = new Realm.RealmBuilder()
                        .setPrincipal(authentication.getUsername())
                        .setPassword(authentication.getPassword())
                        .setUsePreemptiveAuth(authentication.isPreemptive());

            if (authentication.getType() == HttpAuthenticationType.BASIC)
            {
                realmBuilder.setScheme(Realm.AuthScheme.BASIC);
            }
            else if (authentication.getType() == HttpAuthenticationType.DIGEST)
            {
                realmBuilder.setScheme(Realm.AuthScheme.DIGEST);
            }
            else if (authentication.getType() == HttpAuthenticationType.NTLM)
            {
                String domain = authentication.getDomain();
                if (domain != null)
                {
                    realmBuilder.setNtlmDomain(domain);
                }
                String workstation = authentication.getWorkstation();
                String ntlmHost = workstation != null ? workstation : InetAddress.getLocalHost().getHostName();
                realmBuilder.setNtlmHost(ntlmHost).setScheme(Realm.AuthScheme.NTLM);
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

                for (HttpPart part : multipartHttpEntity.getParts())
                {
                    if (part.getFileName() != null)
                    {
                        builder.addBodyPart(new ByteArrayPart(part.getName(), IOUtils.toByteArray(part.getInputStream()), part.getContentType(), null, part.getFileName()));
                    }
                    else
                    {
                        byte[] content = IOUtils.toByteArray(part.getInputStream());
                        builder.addBodyPart(new ByteArrayPart(part.getName(), content, part.getContentType(), null));
                    }
                }
            }
        }

        // Set the response timeout in the request, this value is read by {@code CustomTimeoutThrottleRequestFilter}
        // if the maxConnections attribute is configured in the requester.
        builder.setRequestTimeout(responseTimeout);

        return builder.build();
    }

    @Override
    public void stop()
    {
        asyncHttpClient.close();
    }
}
