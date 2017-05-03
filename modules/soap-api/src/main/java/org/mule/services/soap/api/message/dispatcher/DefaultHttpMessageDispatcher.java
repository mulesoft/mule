/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.api.message.dispatcher;


import static java.util.function.Function.identity;
import static org.mule.service.http.api.HttpConstants.Method.POST;
import static org.mule.service.http.api.HttpHeaders.Names.CONTENT_TYPE;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.util.collection.ImmutableMapCollector;
import org.mule.runtime.extension.api.soap.annotation.Soap;
import org.mule.runtime.extension.api.soap.message.DispatcherResponse;
import org.mule.runtime.extension.api.soap.message.DispatchingContext;
import org.mule.runtime.extension.api.soap.message.MessageDispatcher;
import org.mule.service.http.api.HttpService;
import org.mule.service.http.api.client.HttpClient;
import org.mule.service.http.api.client.HttpClientConfiguration;
import org.mule.service.http.api.client.HttpClientFactory;
import org.mule.service.http.api.domain.ParameterMap;
import org.mule.service.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.api.domain.message.response.HttpResponse;
import org.mule.services.soap.api.exception.DispatchingException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;


/**
 * Default {@link MessageDispatcher} implementation that aims to dispatch messages through HTTP
 * with a prebuilt default configuration.
 *
 * @since 4.0
 */
public final class DefaultHttpMessageDispatcher implements MessageDispatcher {

  private final HttpClient client;

  public DefaultHttpMessageDispatcher(HttpService service) {
    this.client = service.getClientFactory().create(new HttpClientConfiguration.Builder()
                                                            .setName("wsc-http-dispatcher")
                                                            .build());
  }

  @Override
  public void initialise() throws InitialisationException {
    client.start();
  }

  /**
   * {@inheritDoc}
   * <p>
   * Dispatches a Soap message through http adding the SoapAction header, if required, and the content-type.
   */
  @Override
  public DispatcherResponse dispatch(InputStream message, DispatchingContext context) {
    ParameterMap parameters = new ParameterMap();
    parameters.putAll(context.getHeaders());

    HttpRequest request = HttpRequest.builder()
      .setUri(context.getAddress())
      .setMethod(POST)
      .setEntity(new InputStreamHttpEntity(message))
      .setHeaders(parameters)
      .build();

    try {
      HttpResponse response = client.send(request, 5000, false, null);
      InputStream content = ((InputStreamHttpEntity) response.getEntity()).getInputStream();
      return new DispatcherResponse(response.getHeaderValueIgnoreCase(CONTENT_TYPE), content, toHeadersMap(response));
    } catch (IOException e) {
      throw new DispatchingException("An error occurred while sending the SOAP request");
    } catch (TimeoutException e) {
      throw new DispatchingException("The SOAP request timed out", e);
    }
  }

  /**
   * Collects all the headers returned by the http call.
   */
  private Map<String, ? extends List<String>> toHeadersMap(HttpResponse response) {
    return response.getHeaderNames().stream()
      .collect(new ImmutableMapCollector<>(identity(), name -> new ArrayList<>(response.getHeaderValues(name))));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void dispose() {
    client.stop();
  }
}
