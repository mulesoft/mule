/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.transport;


import static org.apache.cxf.message.Message.CONTENT_TYPE;
import static org.mule.extension.ws.api.exception.WscErrors.CANNOT_DISPATCH;
import static org.mule.extension.ws.api.exception.WscErrors.TIMEOUT;
import static org.mule.extension.ws.internal.connection.WscClient.MULE_SOAP_ACTION;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.service.http.api.HttpService;
import org.mule.service.http.api.client.HttpClient;
import org.mule.service.http.api.client.HttpClientConfiguration;
import org.mule.service.http.api.client.HttpClientFactory;
import org.mule.service.http.api.domain.ParameterMap;
import org.mule.service.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.service.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.api.domain.message.response.HttpResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeoutException;

import org.apache.cxf.message.Message;

/**
 * {@link WscDispatcher} implementation that aims to dispatch HTTP messages.
 *
 * @since 4.0
 */
public final class HttpDispatcher implements WscDispatcher {

  private final HttpClient client;
  private final String address;

  public HttpDispatcher(String address, HttpClient client) {
    this.address = address;
    this.client = client;
  }

  /**
   * {@inheritDoc}
   * <p>
   * Dispatches a Soap message through http adding the SoapAction header, if required, and the content-type.
   */
  @Override
  public WscResponse dispatch(Message message) {

    OutputStream os = message.getContent(OutputStream.class);

    HttpRequest request = HttpRequest.builder()
        .setUri(address)
        .setMethod("POST")
        .setEntity(new ByteArrayHttpEntity(os.toString().getBytes()))
        .setHeaders(buildHeaders(message))
        .build();

    try {
      HttpResponse response = client.send(request, 5000, false, null);
      InputStream content = ((InputStreamHttpEntity) response.getEntity()).getInputStream();
      return new WscResponse(content, response.getHeaderValue(CONTENT_TYPE.toLowerCase()));
    } catch (IOException e) {
      throw new ModuleException(e, CANNOT_DISPATCH, "An error occurred while sending the SOAP request");
    } catch (TimeoutException e) {
      throw new ModuleException(e, TIMEOUT, "The SOAP request timed out");
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void dispose() {
    client.stop();
  }

  private ParameterMap buildHeaders(Message message) {
    ParameterMap headers = new ParameterMap();
    headers.put(CONTENT_TYPE, (String) message.get(CONTENT_TYPE));

    String soapAction = (String) message.getExchange().get(MULE_SOAP_ACTION);
    if (soapAction != null) {
      headers.put("SOAPAction", soapAction);
    }
    return headers;
  }

  /**
   * Creates a default {@link HttpDispatcher} using the provided {@link HttpService} and with a fixed set of attributes.
   *
   * @param address the address we want to dispatch http messages to.
   * @param httpService the configured {@link HttpService} used to create the http client.
   * @return a new {@link HttpDispatcher} default instance.
   */
  public static HttpDispatcher createDefault(String address, HttpService httpService) {
    HttpClientFactory clientFactory = httpService.getClientFactory();
    HttpClient client = clientFactory.create(new HttpClientConfiguration.Builder()
        .setThreadNamePrefix("wsc")
        .setOwnerName("wsc")
        .build());
    client.start();
    return new HttpDispatcher(address, client);
  }

}
