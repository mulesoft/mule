/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.impl.transport;


import static java.lang.String.format;
import static org.apache.cxf.message.Message.CONTENT_TYPE;
import static org.mule.service.http.api.HttpConstants.Method.POST;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.service.http.api.HttpService;
import org.mule.service.http.api.client.HttpClient;
import org.mule.service.http.api.client.HttpClientConfiguration;
import org.mule.service.http.api.client.HttpClientFactory;
import org.mule.service.http.api.domain.ParameterMap;
import org.mule.service.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.api.domain.message.response.HttpResponse;
import org.mule.services.soap.api.client.DispatcherResponse;
import org.mule.services.soap.api.client.MessageDispatcher;
import org.mule.services.soap.impl.exception.DispatchingException;

import java.io.IOException;
import java.io.InputStream;
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
  private final String address;

  DefaultHttpMessageDispatcher(String address, HttpClient client) {
    this.address = address;
    this.client = client;
  }

  @Override
  public void initialise() throws InitialisationException {

  }

  /**
   * {@inheritDoc}
   * <p>
   * Dispatches a Soap message through http adding the SoapAction header, if required, and the content-type.
   */
  @Override
  public DispatcherResponse dispatch(InputStream message, Map<String, String> properties) {

    ParameterMap parameters = new ParameterMap();
    parameters.putAll(properties);

    HttpRequest request = HttpRequest.builder()
        .setUri(address)
        .setMethod(POST)
        .setEntity(new InputStreamHttpEntity(message))
        .setHeaders(parameters)
        .build();

    try {
      HttpResponse response = client.send(request, 5000, false, null);
      InputStream content = ((InputStreamHttpEntity) response.getEntity()).getInputStream();
      return new DispatcherResponse(response.getHeaderValueIgnoreCase(CONTENT_TYPE), content);
    } catch (IOException e) {
      throw new DispatchingException("An error occurred while sending the SOAP request");
    } catch (TimeoutException e) {
      throw new DispatchingException("The SOAP request timed out", e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void dispose() {
    client.stop();
  }

  /**
   * Creates a default {@link DefaultHttpMessageDispatcher} using the provided {@link HttpService} and with a fixed set of attributes.
   *
   * @param address the address we want to dispatch http messages to.
   * @param httpService the configured {@link HttpService} used to create the http client.
   * @return a new {@link DefaultHttpMessageDispatcher} default instance.
   */
  public static DefaultHttpMessageDispatcher create(String address, HttpService httpService) {
    String ownerName = format("wsc-default:[%s]", address);
    HttpClientFactory clientFactory = httpService.getClientFactory();
    HttpClient client = clientFactory.create(new HttpClientConfiguration.Builder()
        .setThreadNamePrefix(ownerName)
        .setOwnerName(ownerName)
        .build());
    client.start();
    return new DefaultHttpMessageDispatcher(address, client);
  }
}
