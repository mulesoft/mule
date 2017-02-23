/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.http.impl.service.client;

import static com.ning.http.client.Realm.AuthScheme.NTLM;
import static com.ning.http.client.providers.grizzly.GrizzlyAsyncHttpProviderConfig.Property.DECOMPRESS_RESPONSE;
import static com.ning.http.client.providers.grizzly.GrizzlyAsyncHttpProviderConfig.Property.TRANSPORT_CUSTOMIZER;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.Runtime.getRuntime;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.scheduler.SchedulerConfig.config;
import static org.mule.service.http.api.HttpHeaders.Names.CONNECTION;
import static org.mule.service.http.api.HttpHeaders.Values.CLOSE;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.api.tls.TlsContextTrustStoreConfiguration;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.core.util.StringUtils;
import org.mule.service.http.api.client.HttpAuthenticationType;
import org.mule.service.http.api.client.HttpClient;
import org.mule.service.http.api.client.HttpClientConfiguration;
import org.mule.service.http.api.client.HttpRequestAuthentication;
import org.mule.service.http.api.client.async.ResponseHandler;
import org.mule.service.http.api.client.proxy.NtlmProxyConfig;
import org.mule.service.http.api.client.proxy.ProxyConfig;
import org.mule.service.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.service.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.service.http.api.domain.entity.multipart.HttpPart;
import org.mule.service.http.api.domain.entity.multipart.MultipartHttpEntity;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.api.domain.message.response.HttpResponse;
import org.mule.service.http.api.domain.message.response.HttpResponseBuilder;
import org.mule.service.http.api.tcp.TcpClientSocketProperties;

import com.ning.http.client.AsyncHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.BodyDeferringAsyncHandler;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;
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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrizzlyHttpClient implements HttpClient {

  private static final int MAX_CONNECTION_LIFETIME = 30 * 60 * 1000;

  private static final Logger logger = LoggerFactory.getLogger(GrizzlyHttpClient.class);

  private final TlsContextFactory tlsContextFactory;

  private final ProxyConfig proxyConfig;
  private final TcpClientSocketProperties clientSocketProperties;
  private int maxConnections;
  private boolean usePersistentConnections;
  private int connectionIdleTimeout;

  private String threadNamePrefix;
  private Scheduler selectorScheduler;
  private Scheduler workerScheduler;
  private SchedulerService schedulerService;
  private String ownerName;
  private AsyncHttpClient asyncHttpClient;
  private SSLContext sslContext;


  public GrizzlyHttpClient(HttpClientConfiguration config, SchedulerService schedulerService) {
    this.tlsContextFactory = config.getTlsContextFactory();
    this.proxyConfig = config.getProxyConfig();
    this.clientSocketProperties = config.getClientSocketProperties();
    this.maxConnections = config.getMaxConnections();
    this.usePersistentConnections = config.isUsePersistentConnections();
    this.connectionIdleTimeout = config.getConnectionIdleTimeout();
    this.threadNamePrefix = config.getThreadNamePrefix();
    this.ownerName = config.getOwnerName();

    this.schedulerService = schedulerService;
  }

  @Override
  public void start() {
    selectorScheduler = schedulerService
        .customScheduler(config().withMaxConcurrentTasks(getRuntime().availableProcessors() + 1).withName(threadNamePrefix),
                         MAX_VALUE);
    workerScheduler = schedulerService.ioScheduler();

    AsyncHttpClientConfig.Builder builder = new AsyncHttpClientConfig.Builder();
    builder.setAllowPoolingConnections(true);

    configureTransport(builder);

    configureTlsContext(builder);

    configureProxy(builder);

    configureConnections(builder);

    AsyncHttpClientConfig config = builder.build();

    asyncHttpClient = new AsyncHttpClient(new GrizzlyAsyncHttpProvider(config), config);
  }

  private void configureTlsContext(AsyncHttpClientConfig.Builder builder) {
    if (tlsContextFactory != null) {
      try {
        sslContext = tlsContextFactory.createSslContext();
      } catch (Exception e) {
        throw new MuleRuntimeException(createStaticMessage("Cannot initialize SSL context"), e);
      }

      // This sets all the TLS configuration needed, except for the enabled protocols and cipher suites.
      builder.setSSLContext(sslContext);
      // These complete the set up
      if (tlsContextFactory.getEnabledCipherSuites() != null) {
        builder.setEnabledCipherSuites(tlsContextFactory.getEnabledCipherSuites());
      }
      if (tlsContextFactory.getEnabledProtocols() != null) {
        builder.setEnabledProtocols(tlsContextFactory.getEnabledProtocols());
      }
      TlsContextTrustStoreConfiguration trustStoreConfiguration = tlsContextFactory.getTrustStoreConfiguration();

      if (trustStoreConfiguration != null && trustStoreConfiguration.isInsecure()) {
        logger.warn(String.format(
                                  "TLS configuration for requester %s has been set to use an insecure trust store. This means no certificate validations will be performed, rendering connections vulnerable to attacks. Use at own risk.",
                                  ownerName));
        // This disables hostname verification
        builder.setAcceptAnyCertificate(true);
      }
    }
  }

  private void configureProxy(AsyncHttpClientConfig.Builder builder) {
    if (proxyConfig != null) {
      doConfigureProxy(builder, proxyConfig);
    }
  }

  protected void doConfigureProxy(AsyncHttpClientConfig.Builder builder, ProxyConfig proxyConfig) {
    builder.setProxyServer(buildProxy(proxyConfig));
  }

  protected final ProxyServer buildProxy(ProxyConfig proxyConfig) {
    ProxyServer proxyServer;
    if (!StringUtils.isEmpty(proxyConfig.getUsername())) {
      proxyServer =
          new ProxyServer(proxyConfig.getHost(), proxyConfig.getPort(), proxyConfig.getUsername(), proxyConfig.getPassword());
      if (proxyConfig instanceof NtlmProxyConfig) {
        proxyServer.setNtlmDomain(((NtlmProxyConfig) proxyConfig).getNtlmDomain());
        try {
          proxyServer.setNtlmHost(getHostName());
        } catch (UnknownHostException e) {
          // do nothing, let the default behaviour be used
        }
        proxyServer.setScheme(NTLM);
      }
    } else {
      proxyServer = new ProxyServer(proxyConfig.getHost(), proxyConfig.getPort());
    }
    return proxyServer;
  }

  private void configureTransport(AsyncHttpClientConfig.Builder builder) {
    GrizzlyAsyncHttpProviderConfig providerConfig = new GrizzlyAsyncHttpProviderConfig();
    CompositeTransportCustomizer compositeTransportCustomizer = new CompositeTransportCustomizer();
    compositeTransportCustomizer
        .addTransportCustomizer(new IOStrategyTransportCustomizer(selectorScheduler, workerScheduler));
    compositeTransportCustomizer.addTransportCustomizer(new LoggerTransportCustomizer());

    if (clientSocketProperties != null) {
      compositeTransportCustomizer.addTransportCustomizer(new SocketConfigTransportCustomizer(clientSocketProperties));
      builder.setConnectTimeout(clientSocketProperties.getConnectionTimeout());
    }

    providerConfig.addProperty(TRANSPORT_CUSTOMIZER, compositeTransportCustomizer);
    // Grizzly now decompresses encoded responses, this flag maintains the previous behaviour
    providerConfig.addProperty(DECOMPRESS_RESPONSE, Boolean.FALSE);
    builder.setAsyncHttpClientProviderConfig(providerConfig);
  }

  private void configureConnections(AsyncHttpClientConfig.Builder builder) {
    if (maxConnections > 0) {
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
  public HttpResponse send(HttpRequest request, int responseTimeout, boolean followRedirects,
                           HttpRequestAuthentication authentication)
      throws IOException, TimeoutException {

    Request grizzlyRequest = createGrizzlyRequest(request, responseTimeout, followRedirects, authentication);
    PipedOutputStream outPipe = new PipedOutputStream();
    PipedInputStream inPipe = new PipedInputStream(outPipe);
    BodyDeferringAsyncHandler asyncHandler = new BodyDeferringAsyncHandler(outPipe);
    asyncHttpClient.executeRequest(grizzlyRequest, asyncHandler);
    try {
      Response response = asyncHandler.getResponse();
      return createMuleResponse(response, inPipe);
    } catch (IOException e) {
      if (e.getCause() instanceof TimeoutException) {
        throw (TimeoutException) e.getCause();
      } else if (e.getCause() instanceof IOException) {
        throw (IOException) e.getCause();
      } else {
        throw new IOException(e);
      }
    } catch (InterruptedException e) {
      throw new IOException(e);
    }
  }

  @Override
  public void send(HttpRequest request, int responseTimeout, boolean followRedirects, HttpRequestAuthentication authentication,
                   ResponseHandler handler) {
    try {
      PipedOutputStream outPipe = new PipedOutputStream();
      asyncHttpClient.executeRequest(createGrizzlyRequest(request, responseTimeout, followRedirects, authentication),
                                     new ResponseBodyDeferringAsyncHandler(handler, outPipe));
    } catch (Exception e) {
      handler.onFailure(e);
    }
  }

  private HttpResponse createMuleResponse(Response response, InputStream inputStream) throws IOException {
    HttpResponseBuilder responseBuilder = HttpResponse.builder();
    responseBuilder.setStatusCode(response.getStatusCode());
    responseBuilder.setReasonPhrase(response.getStatusText());
    responseBuilder.setEntity(new InputStreamHttpEntity(inputStream));

    if (response.hasResponseHeaders()) {
      for (String header : response.getHeaders().keySet()) {
        for (String headerValue : response.getHeaders(header)) {
          responseBuilder.addHeader(header, headerValue);
        }
      }
    }
    return responseBuilder.build();
  }

  private Request createGrizzlyRequest(HttpRequest request, int responseTimeout, boolean followRedirects,
                                       HttpRequestAuthentication authentication)
      throws IOException {
    RequestBuilder reqBuilder = createRequestBuilder(request, builder -> {
      builder.setMethod(request.getMethod());
      builder.setFollowRedirects(followRedirects);

      populateHeaders(request, builder);

      for (String queryParamName : request.getQueryParams().keySet()) {
        builder.addQueryParam(queryParamName, request.getQueryParams().get(queryParamName));
      }

      if (authentication != null) {
        Realm.RealmBuilder realmBuilder = new Realm.RealmBuilder().setPrincipal(authentication.getUsername())
            .setPassword(authentication.getPassword()).setUsePreemptiveAuth(authentication.isPreemptive());

        if (authentication.getType() == HttpAuthenticationType.BASIC) {
          realmBuilder.setScheme(Realm.AuthScheme.BASIC);
        } else if (authentication.getType() == HttpAuthenticationType.DIGEST) {
          realmBuilder.setScheme(Realm.AuthScheme.DIGEST);
        } else if (authentication.getType() == HttpAuthenticationType.NTLM) {
          String domain = authentication.getDomain();
          if (domain != null) {
            realmBuilder.setNtlmDomain(domain);
          }
          String workstation = authentication.getWorkstation();
          String ntlmHost = workstation != null ? workstation : getHostName();
          realmBuilder.setNtlmHost(ntlmHost).setScheme(NTLM);
        }

        builder.setRealm(realmBuilder.build());

      }

      if (request.getEntity() != null) {
        if (request.getEntity() instanceof InputStreamHttpEntity) {
          builder.setBody(new InputStreamBodyGenerator(((InputStreamHttpEntity) request.getEntity()).getInputStream()));
        } else if (request.getEntity() instanceof ByteArrayHttpEntity) {
          builder.setBody(((ByteArrayHttpEntity) request.getEntity()).getContent());
        } else if (request.getEntity() instanceof MultipartHttpEntity) {
          MultipartHttpEntity multipartHttpEntity = (MultipartHttpEntity) request.getEntity();

          for (HttpPart part : multipartHttpEntity.getParts()) {
            if (part.getFileName() != null) {
              builder.addBodyPart(new ByteArrayPart(part.getName(), IOUtils.toByteArray(part.getInputStream()),
                                                    part.getContentType(), null, part.getFileName()));
            } else {
              byte[] content = IOUtils.toByteArray(part.getInputStream());
              builder.addBodyPart(new ByteArrayPart(part.getName(), content, part.getContentType(), null));
            }
          }
        }
      }

      // Set the response timeout in the request, this value is read by {@code CustomTimeoutThrottleRequestFilter}
      // if the maxConnections attribute is configured in the requester.
      builder.setRequestTimeout(responseTimeout);
    });

    reqBuilder.setUrl(request.getUri());

    return reqBuilder.build();
  }

  protected RequestBuilder createRequestBuilder(HttpRequest request, RequestConfigurer requestConfigurer) throws IOException {
    final RequestBuilder requestBuilder = new RequestBuilder();
    requestConfigurer.configure(requestBuilder);
    return requestBuilder;
  }

  @FunctionalInterface
  protected interface RequestConfigurer {

    void configure(RequestBuilder reqBuilder) throws IOException;
  }

  protected void populateHeaders(HttpRequest request, RequestBuilder builder) {
    for (String headerName : request.getHeaderNames()) {
      for (String headerValue : request.getHeaderValues(headerName)) {
        builder.addHeader(headerName, headerValue);
      }
    }

    // If persistent connections are disabled, the "Connection: close" header must be explicitly added. AHC will
    // add "Connection: keep-alive" otherwise. (https://github.com/AsyncHttpClient/async-http-client/issues/885)

    if (!usePersistentConnections) {
      String connectionHeaderValue = request.getHeaderValueIgnoreCase(CONNECTION);
      if (connectionHeaderValue != null && !CLOSE.equals(connectionHeaderValue) && logger.isDebugEnabled()) {
        logger.debug("Persistent connections are disabled in the HTTP requester configuration, but the request already "
            + "contains a Connection header with value {}. This header will be ignored, and a Connection: close header "
            + "will be sent instead.", connectionHeaderValue);
      }
      builder.setHeader(CONNECTION, CLOSE);
    }
  }

  private String getHostName() throws UnknownHostException {
    return InetAddress.getLocalHost().getHostName();
  }

  protected ProxyConfig getProxyConfig() {
    return proxyConfig;
  }

  @Override
  public void stop() {
    asyncHttpClient.close();
    workerScheduler.stop(5, SECONDS);
    selectorScheduler.stop(5, SECONDS);
  }

  private class ResponseBodyDeferringAsyncHandler implements AsyncHandler<Response> {

    private volatile Response response;
    private final OutputStream output;
    private final InputStream input;
    private final ResponseHandler responseHandler;
    private final Response.ResponseBuilder responseBuilder = new Response.ResponseBuilder();
    private final AtomicBoolean handled = new AtomicBoolean(false);

    public ResponseBodyDeferringAsyncHandler(ResponseHandler responseHandler, PipedOutputStream output)
        throws IOException {
      this.output = output;
      this.responseHandler = responseHandler;
      this.input = new PipedInputStream(output);
    }

    @Override
    public void onThrowable(Throwable t) {
      try {
        closeOut();
      } catch (IOException e) {
        // ignore
      }
      if (!handled.getAndSet(true)) {
        Exception exception;
        if (t instanceof TimeoutException) {
          exception = (TimeoutException) t;
        } else if (t instanceof IOException) {
          exception = (IOException) t;
        } else {
          exception = new IOException(t);
        }
        responseHandler.onFailure(exception);
      }
    }

    @Override
    public STATE onStatusReceived(HttpResponseStatus responseStatus) throws Exception {
      responseBuilder.reset();
      responseBuilder.accumulate(responseStatus);
      return STATE.CONTINUE;
    }

    @Override
    public STATE onHeadersReceived(HttpResponseHeaders headers) throws Exception {
      responseBuilder.accumulate(headers);
      return STATE.CONTINUE;
    }

    @Override
    public STATE onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
      // body arrived, can handle the partial response
      handleIfNecessary();
      bodyPart.writeTo(output);
      return STATE.CONTINUE;
    }

    protected void closeOut() throws IOException {
      try {
        output.flush();
      } finally {
        output.close();
      }
    }

    @Override
    public Response onCompleted() throws IOException {
      // there may have been no body, handle partial response
      handleIfNecessary();
      closeOut();
      return null;
    }

    private void handleIfNecessary() {
      if (!handled.getAndSet(true)) {
        response = responseBuilder.build();
        try {
          responseHandler.onCompletion(createMuleResponse(response, input));
        } catch (IOException e) {
          responseHandler.onFailure(e);
        }
      }
    }
  }

}
