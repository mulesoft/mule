/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.request.grizzly;

import static com.ning.http.client.Realm.AuthScheme.NTLM;
import static com.ning.http.client.providers.grizzly.GrizzlyAsyncHttpProviderConfig.Property.MAX_HTTP_PACKET_HEADER_SIZE;
import static java.lang.Boolean.getBoolean;
import static java.lang.Integer.getInteger;
import static java.lang.Integer.valueOf;
import static java.lang.System.getProperties;
import static java.lang.System.getProperty;
import static org.glassfish.grizzly.http.HttpCodecFilter.DEFAULT_MAX_HTTP_PACKET_HEADER_SIZE;
import static org.mule.api.config.MuleProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.module.http.api.HttpConstants.HttpProperties.GRIZZLY_MEMORY_MANAGER_SYSTEM_PROPERTY;
import static org.mule.module.http.api.HttpHeaders.Names.CONNECTION;
import static org.mule.module.http.api.HttpHeaders.Names.CONTENT_DISPOSITION;
import static org.mule.module.http.api.HttpHeaders.Names.CONTENT_ID;
import static org.mule.module.http.api.HttpHeaders.Names.CONTENT_LENGTH;
import static org.mule.module.http.api.HttpHeaders.Names.CONTENT_TRANSFER_ENCODING;
import static org.mule.module.http.api.HttpHeaders.Names.CONTENT_TYPE;
import static org.mule.module.http.api.HttpHeaders.Values.CLOSE;
import static org.mule.module.http.internal.request.RequestResourcesUtils.closeResources;
import org.mule.api.CompletionHandler;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.context.WorkManager;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleUtils;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.http.api.requester.proxy.ProxyConfig;
import org.mule.module.http.internal.domain.ByteArrayHttpEntity;
import org.mule.module.http.internal.domain.EmptyHttpEntity;
import org.mule.module.http.internal.domain.HttpEntity;
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
import org.mule.module.http.internal.request.MuleBodyDeferringAsyncHandler;
import org.mule.module.http.internal.request.NtlmProxyConfig;
import org.mule.transport.ssl.api.TlsContextFactory;
import org.mule.transport.ssl.api.TlsContextTrustStoreConfiguration;
import org.mule.transport.tcp.TcpClientSocketProperties;
import org.mule.util.IOUtils;
import org.mule.util.StringUtils;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.BodyDeferringAsyncHandler;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.ProxyServer;
import com.ning.http.client.Realm;
import com.ning.http.client.Request;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.Response;
import com.ning.http.client.generators.InputStreamBodyGenerator;
import com.ning.http.client.providers.grizzly.FeedableBodyGenerator;
import com.ning.http.client.providers.grizzly.GrizzlyAsyncHttpProvider;
import com.ning.http.client.providers.grizzly.GrizzlyAsyncHttpProviderConfig;
import com.ning.http.client.providers.grizzly.NonBlockingInputStreamFeeder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrizzlyHttpClient implements HttpClient
{

    private static final int MAX_CONNECTION_LIFETIME = 30 * 60 * 1000;

    public static final String CUSTOM_MAX_HTTP_PACKET_HEADER_SIZE = SYSTEM_PROPERTY_PREFIX + "http.client.headerSectionSize";

    public static final String AVOID_ZERO_CONTENT_LENGTH = SYSTEM_PROPERTY_PREFIX + "http.client.avoidZeroContentLength";

    private static final String ENABLE_REQUEST_STREAMING_PROPERTY_NAME = SYSTEM_PROPERTY_PREFIX + "http.requestStreaming.enable";
    private static boolean requestStreamingEnabled = getProperties().containsKey(ENABLE_REQUEST_STREAMING_PROPERTY_NAME);

    private static final int DEFAULT_REQUEST_STREAMING_BUFFER_SIZE = 8 * 1024;
    private static final String REQUEST_STREAMING_BUFFER_LEN_PROPERTY_NAME =
        SYSTEM_PROPERTY_PREFIX + "http.requestStreaming.bufferSize";
    private static int requestStreamingBufferSize =
        getInteger(REQUEST_STREAMING_BUFFER_LEN_PROPERTY_NAME, DEFAULT_REQUEST_STREAMING_BUFFER_SIZE);

    private static final Logger logger = LoggerFactory.getLogger(GrizzlyHttpClient.class);

    private static final List<String> SPECIAL_CUSTOM_HEADERS = Arrays.asList(
            CONTENT_DISPOSITION.toLowerCase(),
            CONTENT_TRANSFER_ENCODING.toLowerCase(),
            CONTENT_TYPE.toLowerCase(),
            CONTENT_ID.toLowerCase());

    public static final String HOST_SEPARATOR = ",";

    private final TlsContextFactory tlsContextFactory;
    private final TlsContextFactory defaultTlsContextFactory;
    private final ProxyConfig proxyConfig;
    private final TcpClientSocketProperties clientSocketProperties;

    private int maxConnections;
    private boolean usePersistentConnections;
    private int connectionIdleTimeout;
    private final boolean streaming;
    private int responseBufferSize;
    private String threadNamePrefix;
    private final Integer kernelCoreSize;
    private final Integer maxKernelCoreSize;
    private final Integer workerCoreSize;
    private final Integer maxWorkerCoreSize;
    private final Integer selectorRunnerCoreSize;
    private String ownerName;

    private AsyncHttpClient asyncHttpClient;
    private SSLContext sslContext;
    private boolean avoidZeroContentLength = getBoolean(AVOID_ZERO_CONTENT_LENGTH);

    public GrizzlyHttpClient(HttpClientConfiguration config)
    {
        this.tlsContextFactory = config.getTlsContextFactory();
        this.defaultTlsContextFactory = config.getDefaultTlsContextFactory();
        this.proxyConfig = config.getProxyConfig();
        this.clientSocketProperties = config.getClientSocketProperties();
        this.maxConnections = config.getMaxConnections();
        this.usePersistentConnections = config.isUsePersistentConnections();
        this.connectionIdleTimeout = config.getConnectionIdleTimeout();
        this.streaming = config.isStreaming();
        this.responseBufferSize = config.getResponseBufferSize();
        this.threadNamePrefix = config.getThreadNamePrefix();
        this.ownerName = config.getOwnerName();
        this.kernelCoreSize = config.getKernelCoreSize();
        this.maxKernelCoreSize = config.getMaxKernelPoolSize();
        this.workerCoreSize = config.getWorkerCoreSize();
        this.maxWorkerCoreSize = config.getMaxWorkerPoolSize();
        this.selectorRunnerCoreSize = config.getSelectorRunnersCount();
    }

    @Override
    public void start() throws MuleException
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

    private void configureTlsContext(AsyncHttpClientConfig.Builder builder) throws MuleException
    {
        TlsContextFactory resolvedTlsContext = defaultTlsContextFactory;
        if (tlsContextFactory != null)
        {
            resolvedTlsContext = tlsContextFactory;
            LifecycleUtils.initialiseIfNeeded(tlsContextFactory);
            try
            {
                sslContext = tlsContextFactory.createSslContext();
            }
            catch (Exception e)
            {
                throw new DefaultMuleException(createStaticMessage("Cannot initialize SSL context"), e);
            }

            // This sets all the TLS configuration needed, except for the enabled protocols and cipher suites.
            builder.setSSLContext(sslContext);
            TlsContextTrustStoreConfiguration trustStoreConfiguration = tlsContextFactory.getTrustStoreConfiguration();
            if(trustStoreConfiguration != null && trustStoreConfiguration.isInsecure())
            {
                logger.warn(String.format("TLS configuration for requester %s has been set to use an insecure trust store. This means no certificate validations will be performed, rendering connections vulnerable to attacks. Use at own risk.", ownerName));
                //This disables hostname verification
                builder.setAcceptAnyCertificate(true);
            }
        }
        //These complete the set up, they must always be set in case an implicit SSL connection is used
        if (resolvedTlsContext.getEnabledCipherSuites() != null)
        {
            builder.setEnabledCipherSuites(resolvedTlsContext.getEnabledCipherSuites());
        }
        if (resolvedTlsContext.getEnabledProtocols() != null)
        {
            builder.setEnabledProtocols(resolvedTlsContext.getEnabledProtocols());
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

        if (proxyConfig.getNonProxyHosts() != null && !proxyConfig.getNonProxyHosts().isEmpty())
        {
            for (final String host : proxyConfig.getNonProxyHosts().split(HOST_SEPARATOR))
            {
                proxyServer.addNonProxyHost(host.trim());
            }
        }
        return proxyServer;
    }

    private void configureTransport(AsyncHttpClientConfig.Builder builder)
    {
        GrizzlyAsyncHttpProviderConfig providerConfig = new GrizzlyAsyncHttpProviderConfig();
        CompositeTransportCustomizer compositeTransportCustomizer = new CompositeTransportCustomizer();
        compositeTransportCustomizer.addTransportCustomizer(new IOStrategyTransportCustomizer(threadNamePrefix, maxWorkerCoreSize, workerCoreSize, maxKernelCoreSize, kernelCoreSize, selectorRunnerCoreSize));
        compositeTransportCustomizer.addTransportCustomizer(new LoggerTransportCustomizer());

        if (getProperty(GRIZZLY_MEMORY_MANAGER_SYSTEM_PROPERTY) == null)
        {
            compositeTransportCustomizer.addTransportCustomizer(new MemoryManagerTransportCustomizer());
        }

        if (clientSocketProperties != null)
        {
            compositeTransportCustomizer.addTransportCustomizer(new SocketConfigTransportCustomizer(clientSocketProperties));
            builder.setConnectTimeout(clientSocketProperties.getConnectionTimeout());
        }

        providerConfig.addProperty(GrizzlyAsyncHttpProviderConfig.Property.TRANSPORT_CUSTOMIZER, compositeTransportCustomizer);
        //Grizzly now decompresses encoded responses, this flag maintains the previous behaviour
        providerConfig.addProperty(GrizzlyAsyncHttpProviderConfig.Property.DECOMPRESS_RESPONSE, Boolean.FALSE);
        providerConfig.addProperty(MAX_HTTP_PACKET_HEADER_SIZE, retrieveMaximumHeaderSectionSize());
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
        if (streaming)
        {
            return sendAndDefer(request, responseTimeout, followRedirects, authentication);
        }
        else
        {
            return sendAndWait(request, responseTimeout, followRedirects, authentication);
        }
    }

    /**
     * Blocking send which uses a {@link PipedOutputStream} to populate the HTTP response as it arrives and propagates a
     * {@link PipedInputStream} as soon as the response headers are parsed.
     * <p/>
     * Because of the internal buffer used to hold the arriving chunks, the response MUST be eventually read or the worker threads
     * will block waiting to allocate them. Likewise, read/write speed differences could cause issues. The buffer size can be
     * customized for these reason.
     */
    public HttpResponse sendAndDefer(HttpRequest request, int responseTimeout, boolean followRedirects,
                                     HttpRequestAuthentication authentication) throws IOException, TimeoutException
    {
        Request grizzlyRequest = createGrizzlyRequest(request, responseTimeout, followRedirects, authentication);
        PipedOutputStream outPipe = new PipedOutputStream();
        PipedInputStream inPipe = new PipedInputStream(outPipe, responseBufferSize);
        BodyDeferringAsyncHandler asyncHandler = new MuleBodyDeferringAsyncHandler(outPipe, request);
        asyncHttpClient.executeRequest(grizzlyRequest, asyncHandler);
        try
        {
            Response response = asyncHandler.getResponse();
            return createMuleResponse(response, inPipe);
        }
        catch (IOException e)
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
        catch (InterruptedException e)
        {
            throw new IOException(e);
        }
    }

    /**
     * Blocking send which waits to load the whole response to memory before propagating it.
     */
    public HttpResponse sendAndWait(HttpRequest request, int responseTimeout, boolean followRedirects,
                                    HttpRequestAuthentication authentication) throws IOException, TimeoutException
    {
        Request grizzlyRequest = createGrizzlyRequest(request, responseTimeout, followRedirects, authentication);
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
            return createMuleResponse(response, response.getResponseBodyAsStream());
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
        finally
        {
            closeResources(request);
        }
    }

    @Override
    public void send(HttpRequest request, int responseTimeout, boolean followRedirects, HttpRequestAuthentication
            authentication, final CompletionHandler<HttpResponse, Exception> completionHandler, WorkManager workManager)
    {
        try
        {
            AsyncHandler handler;
            if (streaming)
            {
                handler = new WorkManagerBodyDeferringAsyncHandler(completionHandler, workManager, new PipedOutputStream(), request);
            }
            else
            {
                handler = new WorkManagerAsyncCompletionHandler(completionHandler, workManager, request);
            }
            asyncHttpClient.executeRequest(createGrizzlyRequest(request, responseTimeout, followRedirects, authentication), handler);
        }
        catch (Exception e)
        {
            completionHandler.onFailure(e);
        }
    }

    protected HttpResponse createMuleResponse(Response response, InputStream inputStream) throws IOException
    {
        HttpResponseBuilder responseBuilder = new HttpResponseBuilder();
        responseBuilder.setStatusCode(response.getStatusCode());
        responseBuilder.setReasonPhrase(response.getStatusText());
        responseBuilder.setEntity(getEntity(response, inputStream));

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

    private HttpEntity getEntity(Response response, InputStream inputStream) {
        String length = response.getHeader(CONTENT_LENGTH);
        if (length != null && "0".equals(length))
        {
            return new EmptyHttpEntity();
        }
        return new InputStreamHttpEntity(inputStream);
    }

    private Request createGrizzlyRequest(final HttpRequest request, final int responseTimeout, final boolean followRedirects,
                                         final HttpRequestAuthentication authentication)
            throws IOException
    {
        RequestBuilder builder = createRequestBuilder(request, new RequestConfigurer()
        {
            @Override
            public void configure(RequestBuilder builder) throws IOException
            {
                builder.setMethod(request.getMethod());
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
                                                                              .setUsePreemptiveAuth(authentication.isPreemptive())
                                                                              .setForceConnectionClose(authentication.credentialsMayVary());


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
                        setStreamingBodyToRequestBuilder((InputStreamHttpEntity) request.getEntity(), builder);
                    }
                    else if (request.getEntity() instanceof ByteArrayHttpEntity)
                    {
                        ByteArrayHttpEntity byteArrayHttpEntity = (ByteArrayHttpEntity) request.getEntity();
                        if (byteArrayHttpEntity.getContent().length != 0 || (byteArrayHttpEntity.getContent().length == 0 && !avoidZeroContentLength))
                        {
                            builder.setBody(((ByteArrayHttpEntity) request.getEntity()).getContent());
                        }
                    }
                    else if (request.getEntity() instanceof MultipartHttpEntity)
                    {
                        MultipartHttpEntity multipartHttpEntity = (MultipartHttpEntity) request.getEntity();

                        for (HttpPart part : multipartHttpEntity.getParts())
                        {
                            ByteArrayPart byteArrayPart;
                            String encoding = null;
                            String contentId = null;

                            for (String headerName : part.getHeaderNames())
                            {
                                if (headerName.toLowerCase().equals(CONTENT_TRANSFER_ENCODING.toLowerCase()))
                                {
                                    encoding = part.getHeader(headerName);
                                }
                                else if (headerName.toLowerCase().equals(CONTENT_ID.toLowerCase()))
                                {
                                    contentId = part.getHeader(headerName);
                                }
                            }

                            byte[] content = IOUtils.toByteArray(part.getInputStream());
                            byteArrayPart = new ByteArrayPart(part.getName(), content, part.getContentType(), null, part.getFileName(), contentId, encoding);

                            for (String headerName : part.getHeaderNames())
                            {
                                if (!SPECIAL_CUSTOM_HEADERS.contains(headerName.toLowerCase()))
                                {
                                    byteArrayPart.addCustomHeader(headerName + ": ", part.getHeader(headerName));
                                }
                                else if (headerName.toLowerCase().equals(CONTENT_DISPOSITION.toLowerCase()))
                                {
                                    byteArrayPart.setCustomContentDisposition(part.getHeader(headerName));
                                }
                                else if (headerName.toLowerCase().equals(CONTENT_TYPE.toLowerCase()))
                                {
                                    byteArrayPart.setCustomContentType(part.getHeader(headerName));
                                }
                            }

                            builder.addBodyPart(byteArrayPart);
                        }
                    }
                }

                // Set the response timeout in the request, this value is read by {@code
                // CustomTimeoutThrottleRequestFilter}
                // if the maxConnections attribute is configured in the requester.
                builder.setRequestTimeout(responseTimeout);
            }
        });

        builder.setUrl(request.getUri());

        return builder.build();
    }

    private void setStreamingBodyToRequestBuilder(InputStreamHttpEntity entity, RequestBuilder builder) throws IOException
    {
        if (isRequestStreamingEnabled())
        {
            FeedableBodyGenerator bodyGenerator = new FeedableBodyGenerator();
            FeedableBodyGenerator.Feeder nonBlockingFeeder =
                new NonBlockingInputStreamFeeder(bodyGenerator, entity.getInputStream(), requestStreamingBufferSize);
            bodyGenerator.setFeeder(nonBlockingFeeder);
            builder.setBody(bodyGenerator);
        }
        else
        {
            builder.setBody(new InputStreamBodyGenerator(entity.getInputStream()));
        }
    }

    protected RequestBuilder createRequestBuilder(HttpRequest request, RequestConfigurer requestConfigurer) throws IOException
    {
        final RequestBuilder requestBuilder = new RequestBuilder();
        requestConfigurer.configure(requestBuilder);
        return requestBuilder;
    }

    protected interface RequestConfigurer
    {
        void configure(RequestBuilder reqBuilder) throws IOException;
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

    private class WorkManagerAsyncCompletionHandler extends AsyncCompletionHandler<Response>
    {

        private CompletionHandler<HttpResponse, Exception> completionHandler;
        private WorkManager workManager;
        private HttpRequest request;

        WorkManagerAsyncCompletionHandler(CompletionHandler<HttpResponse, Exception> completionHandler,
                WorkManager workManager, HttpRequest request)
        {
            this.completionHandler = completionHandler;
            this.workManager = workManager;
            this.request = request;
        }

        @Override
        public Response onCompleted(final Response response) throws Exception
        {
            workManager.execute(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        completionHandler.onCompletion(createMuleResponse(response, response.getResponseBodyAsStream()));
                    }
                    catch (IOException e)
                    {
                        completionHandler.onFailure(e);
                    }
                    finally
                    {
                        closeResources(request);
                    }
                }
            });
            return null;
        }

        @Override
        public void onThrowable(final Throwable t)
        {
            workManager.execute(new Runnable()
            {
                @Override
                public void run()
                {
                    completionHandler.onFailure((Exception) t);
                }
            });

        }
    }

    private class WorkManagerBodyDeferringAsyncHandler implements AsyncHandler<Response>
    {
        private volatile Response response;
        private final HttpRequest request;
        private final OutputStream output;
        private final InputStream input;
        private final WorkManager workManager;
        private final CompletionHandler<HttpResponse, Exception> completionHandler;
        private final Response.ResponseBuilder responseBuilder = new Response.ResponseBuilder();
        private final AtomicBoolean handled = new AtomicBoolean(false);

        public WorkManagerBodyDeferringAsyncHandler(CompletionHandler<HttpResponse, Exception> completionHandler, WorkManager workManager, PipedOutputStream output, HttpRequest request)
                throws IOException
        {
            this.output = output;
            this.workManager = workManager;
            this.completionHandler = completionHandler;
            this.input = new PipedInputStream(output, responseBufferSize);
            this.request = request;
        }

        public void onThrowable(final Throwable t)
        {
            try
            {
                closeOut();
            }
            catch (IOException e)
            {
                // ignore
            }
            if (!handled.getAndSet(true))
            {
                workManager.execute(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        completionHandler.onFailure((Exception) t);
                    }
                });
            }
        }

        public STATE onStatusReceived(HttpResponseStatus responseStatus) throws Exception
        {
            responseBuilder.reset();
            responseBuilder.accumulate(responseStatus);
            return STATE.CONTINUE;
        }

        public STATE onHeadersReceived(HttpResponseHeaders headers) throws Exception
        {
            responseBuilder.accumulate(headers);
            return STATE.CONTINUE;
        }

        public STATE onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception
        {
            //body arrived, can handle the partial response
            handleIfNecessary();
            bodyPart.writeTo(output);
            return STATE.CONTINUE;
        }

        protected void closeOut() throws IOException
        {
            closeResources(request);
            
            try
            {
                output.flush();
            }
            finally
            {
                output.close();
            }
        }

        public Response onCompleted() throws IOException
        {
            //there may have been no body, handle partial response
            handleIfNecessary();
            closeOut();
            return null;
        }

        private void handleIfNecessary()
        {
            if (!handled.getAndSet(true))
            {
                response = responseBuilder.build();
                workManager.execute(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            completionHandler.onCompletion(createMuleResponse(response, input));
                        }
                        catch (IOException e)
                        {
                            completionHandler.onFailure(e);
                        }
                    }
                });
            }
        }
    }

    @Override
    public InputStream sendAndReceiveInputStream(HttpRequest request, int responseTimeout, boolean followRedirects,
                                                 HttpRequestAuthentication authentication) throws IOException, TimeoutException
    {
        Request grizzlyRequest = createGrizzlyRequest(request, responseTimeout, followRedirects, authentication);
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
            return response.getResponseBodyAsStream();
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

    private int retrieveMaximumHeaderSectionSize()
    {
        try
        {
            return valueOf(getProperty(CUSTOM_MAX_HTTP_PACKET_HEADER_SIZE, String.valueOf(DEFAULT_MAX_HTTP_PACKET_HEADER_SIZE)));
        }
        catch (NumberFormatException e)
        {
            throw new MuleRuntimeException(CoreMessages.createStaticMessage(String.format("Invalid value %s for %s configuration.", getProperty(CUSTOM_MAX_HTTP_PACKET_HEADER_SIZE), CUSTOM_MAX_HTTP_PACKET_HEADER_SIZE)), e);
        }
    }

    private static boolean isRequestStreamingEnabled() {
        return requestStreamingEnabled;
    }
}
