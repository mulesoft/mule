/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.request.grizzly;

import static com.ning.http.client.Realm.AuthScheme.NTLM;
import static org.mule.module.http.api.HttpHeaders.Names.CONNECTION;
import static org.mule.module.http.api.HttpHeaders.Values.CLOSE;
import org.mule.api.CompletionHandler;
import org.mule.api.MuleException;
import org.mule.api.context.WorkManager;
import org.mule.api.context.WorkManagerSource;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleUtils;
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
import org.mule.module.http.internal.request.HttpClientConfiguration;
import org.mule.module.http.internal.request.NtlmProxyConfig;
import org.mule.transport.ssl.api.TlsContextFactory;
import org.mule.transport.ssl.api.TlsContextTrustStoreConfiguration;
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
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.SSLContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrizzlyHttpClient implements HttpClient
{

    private static final int MAX_CONNECTION_LIFETIME = 30 * 60 * 1000;

    private static final Logger logger = LoggerFactory.getLogger(GrizzlyHttpClient.class);

    private final TlsContextFactory tlsContextFactory;
    private final ProxyConfig proxyConfig;
    private final TcpClientSocketProperties clientSocketProperties;

    private int maxConnections;
    private boolean usePersistentConnections;
    private int connectionIdleTimeout;
    private String threadNamePrefix;
    private String ownerName;

    private AsyncHttpClient asyncHttpClient;
    private SSLContext sslContext;

    public GrizzlyHttpClient(HttpClientConfiguration config)
    {
        this.tlsContextFactory = config.getTlsContextFactory();
        this.proxyConfig = config.getProxyConfig();
        this.clientSocketProperties = config.getClientSocketProperties();
        this.maxConnections = config.getMaxConnections();
        this.usePersistentConnections = config.isUsePersistentConnections();
        this.connectionIdleTimeout = config.getConnectionIdleTimeout();
        this.threadNamePrefix = config.getThreadNamePrefix();
        this.ownerName = config.getOwnerName();
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
            LifecycleUtils.initialiseIfNeeded(tlsContextFactory);
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
            TlsContextTrustStoreConfiguration trustStoreConfiguration = tlsContextFactory.getTrustStoreConfiguration();
            if(trustStoreConfiguration != null && trustStoreConfiguration.isInsecure())
            {
                logger.warn(String.format("TLS configuration for requester %s has been set to use an insecure trust store. This means no certificate validations will be performed, rendering connections vulnerable to attacks. Use at own risk.", ownerName));
                //This disables hostname verification
                builder.setAcceptAnyCertificate(true);
            }
        }
    }

    private void configureProxy(AsyncHttpClientConfig.Builder builder)
    {
        if (proxyConfig != null)
        {
            doConfigureProxy(builder, proxyConfig);
        }
    }

    protected void doConfigureProxy(AsyncHttpClientConfig.Builder builder, ProxyConfig proxyConfig)
    {
        builder.setProxyServer(buildProxy(proxyConfig));
    }

    protected final ProxyServer buildProxy(ProxyConfig proxyConfig)
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
                try
                {
                    proxyServer.setNtlmHost(getHostName());
                }
                catch (UnknownHostException e)
                {
                    //do nothing, let the default behaviour be used
                }
                proxyServer.setScheme(NTLM);
            }
        }
        else
        {
            proxyServer = new ProxyServer(proxyConfig.getHost(),proxyConfig.getPort());
        }
        return proxyServer;
    }

    private void configureTransport(AsyncHttpClientConfig.Builder builder)
    {
        GrizzlyAsyncHttpProviderConfig providerConfig = new GrizzlyAsyncHttpProviderConfig();
        CompositeTransportCustomizer compositeTransportCustomizer = new CompositeTransportCustomizer();
        compositeTransportCustomizer.addTransportCustomizer(new IOStrategyTransportCustomizer
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
            // No timeout is used to get the value of the future object, as the responseTimeout configured in the request that
            // is being sent will make the call throw a {@code TimeoutException} if this time is exceeded.
            Response response = future.get();

            // Under high load, sometimes the get() method returns null. Retrying once fixes the problem (see MULE-8712).
            if (response == null)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Null response returned by async client");
                }
                response = future.get();
            }
            return createMuleResponse(response);
        }
        catch (InterruptedException e)
        {
            throw new IOException(e);
        }
        catch (ExecutionException e)
        {
            if (e.getCause() instanceof TimeoutException)
            {
                throw (TimeoutException) e.getCause();
            }
            else if (e.getCause() instanceof IOException)
            {
                throw (IOException) e.getCause();
            }
            else
            {
                throw new IOException(e);
            }
        }
    }

    @Override
    public void send(HttpRequest request, int responseTimeout, boolean followRedirects, HttpRequestAuthentication
            authentication, final CompletionHandler<HttpResponse, Exception> completionHandler, WorkManager workManager)
    {
        try
        {
            asyncHttpClient.executeRequest(createGrizzlyRequest(request, responseTimeout, followRedirects, authentication),
                                           new WorkManagerSourceAsyncCompletionHandler(completionHandler, workManager));
        }
        catch (Exception e)
        {
            completionHandler.onFailure(e);
        }
    }

    private class WorkManagerSourceAsyncCompletionHandler extends AsyncCompletionHandler<Response> implements WorkManagerSource
    {

        private CompletionHandler<HttpResponse, Exception> completionHandler;
        private WorkManager workManager;

        WorkManagerSourceAsyncCompletionHandler(CompletionHandler<HttpResponse, Exception> completionHandler,
                                                WorkManager workManager)
        {
            this.completionHandler = completionHandler;
            this.workManager = workManager;
        }

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

        @Override
        public WorkManager getWorkManager() throws MuleException
        {
            return workManager;
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
        RequestBuilder builder = createRequestBuilder(request);

        builder.setMethod(request.getMethod());
        builder.setUrl(request.getUri());
        builder.setFollowRedirects(followRedirects);

        populateHeaders(request, builder);

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
                String ntlmHost = workstation != null ? workstation : getHostName();
                realmBuilder.setNtlmHost(ntlmHost).setScheme(NTLM);
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

    protected RequestBuilder createRequestBuilder(HttpRequest request)
    {
        return new RequestBuilder();
    }

    protected void populateHeaders(HttpRequest request, RequestBuilder builder)
    {
        for (String headerName : request.getHeaderNames())
        {
            for (String headerValue : request.getHeaderValues(headerName))
            {
                builder.addHeader(headerName, headerValue);
            }
        }

        // If persistent connections are disabled, the "Connection: close" header must be explicitly added. AHC will
        // add "Connection: keep-alive" otherwise. (https://github.com/AsyncHttpClient/async-http-client/issues/885)

        if (!usePersistentConnections)
        {
            String connectionHeaderValue = request.getHeaderValueIgnoreCase(CONNECTION);
            if (connectionHeaderValue != null && !CLOSE.equals(connectionHeaderValue) && logger.isDebugEnabled())
            {
                logger.debug("Persistent connections are disabled in the HTTP requester configuration, but the request already " +
                             "contains a Connection header with value {}. This header will be ignored, and a Connection: close header " +
                             "will be sent instead.",
                        connectionHeaderValue);
            }
            builder.setHeader(CONNECTION, CLOSE);
        }
    }

    private String getHostName() throws UnknownHostException
    {
        return InetAddress.getLocalHost().getHostName();
    }

    protected ProxyConfig getProxyConfig()
    {
        return proxyConfig;
    }

    @Override
    public void stop()
    {
        asyncHttpClient.close();
    }
}
