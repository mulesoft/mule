/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.soap.api.message.dispatcher;


import static java.util.function.Function.identity;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static org.mule.runtime.http.api.HttpConstants.Method.POST;
import static org.mule.runtime.http.api.HttpHeaders.Names.CONTENT_TYPE;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.soap.message.DispatchingRequest;
import org.mule.runtime.extension.api.soap.message.DispatchingResponse;
import org.mule.runtime.extension.api.soap.message.MessageDispatcher;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.client.HttpClient;
import org.mule.runtime.http.api.client.HttpClientConfiguration;
import org.mule.runtime.http.api.domain.ParameterMap;
import org.mule.runtime.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;
import org.mule.runtime.soap.api.exception.DispatchingException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;


/**
 * Default {@link MessageDispatcher} implementation that aims to dispatch messages through HTTP
 * with a prebuilt default configuration.
 *
 * @since 4.0
 */
public final class DefaultHttpMessageDispatcher implements MessageDispatcher {

  private static final Logger LOGGER = Logger.getLogger(DefaultHttpMessageDispatcher.class);

  private final HttpClient client;

  public DefaultHttpMessageDispatcher(HttpService service) {
    this.client = service.getClientFactory().create(new HttpClientConfiguration.Builder()
        .setName("wsc-dispatcher")
        .build());
    log("Creating http client [" + client + "]");
  }

  @Override
  public void initialise() throws InitialisationException {
    log("Starting client [" + client + "]");
    client.start();
  }

  /**
   * {@inheritDoc}
   * <p>
   * Dispatches a Soap message through http adding the SoapAction header, if required, and the content-type.
   */
  @Override
  public DispatchingResponse dispatch(DispatchingRequest context) {
    ParameterMap parameters = new ParameterMap();
    context.getHeaders().forEach(parameters::put);

    // It's important that content type is bundled with the headers
    parameters.put(CONTENT_TYPE, context.getContentType());

    HttpRequest request = HttpRequest.builder()
        .setUri(context.getAddress())
        .setMethod(POST)
        .setEntity(new InputStreamHttpEntity(context.getContent()))
        .setHeaders(parameters)
        .build();

    try {
      HttpResponse response = client.send(request, 5000, false, null);
      InputStream content = ((InputStreamHttpEntity) response.getEntity()).getInputStream();
      return new DispatchingResponse(content, response.getHeaderValueIgnoreCase(CONTENT_TYPE), toHeadersMap(response));
    } catch (IOException e) {
      throw new DispatchingException("An error occurred while sending the SOAP request");
    } catch (TimeoutException e) {
      throw new DispatchingException("The SOAP request timed out", e);
    }
  }

  /**
   * Collects all the headers returned by the http call.
   */
  private Map<String, String> toHeadersMap(HttpResponse response) {
    return response.getHeaderNames().stream()
        .collect(toMap(identity(), name -> response.getHeaderValues(name).stream().collect(joining(" "))));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void dispose() {
    log("Stopping http client [" + client + "]");
    client.stop();
  }

  private void log(String message) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(message);
    }
  }
}
