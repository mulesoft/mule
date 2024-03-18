/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.soap.api.message.dispatcher;


import static org.mule.runtime.http.api.HttpConstants.Method.POST;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

import org.mule.runtime.api.util.MultiMap;
import org.mule.runtime.extension.api.soap.message.DispatchingRequest;
import org.mule.runtime.extension.api.soap.message.DispatchingResponse;
import org.mule.runtime.extension.api.soap.message.MessageDispatcher;
import org.mule.runtime.http.api.client.HttpClient;
import org.mule.runtime.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;
import org.mule.runtime.soap.api.exception.DispatchingException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Default {@link MessageDispatcher} implementation that aims to dispatch messages through HTTP with a prebuilt default
 * configuration.
 *
 * @since 4.0
 */
public final class DefaultHttpMessageDispatcher implements MessageDispatcher {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultHttpMessageDispatcher.class.getName());
  private static final int DEFAULT_TIMEOUT_MILLIS = 5000;

  private final HttpClient client;

  public DefaultHttpMessageDispatcher(HttpClient client) {
    this.client = client;
  }

  /**
   * {@inheritDoc}
   * <p>
   * Dispatches a Soap message through http adding the SoapAction header, if required, and the content-type.
   */
  @Override
  public DispatchingResponse dispatch(DispatchingRequest context) {
    InputStream content = logIfNeeded("Soap Request to [" + context.getAddress() + "]", context.getContent());
    HttpRequest request = HttpRequest.builder()
        .uri(context.getAddress())
        .method(POST)
        .entity(new InputStreamHttpEntity(content))
        .headers(new MultiMap<>(context.getHeaders()))
        .build();
    try {
      HttpResponse response = client.send(request, DEFAULT_TIMEOUT_MILLIS, false, null);
      return new DispatchingResponse(logIfNeeded("Soap Response", response.getEntity().getContent()), toHeadersMap(response));
    } catch (IOException e) {
      throw new DispatchingException("An error occurred while sending the SOAP request", e);
    } catch (TimeoutException e) {
      throw new DispatchingException("The SOAP request timed out", e);
    }
  }

  /**
   * Logs the content if it's log enabled, returns the same content.
   */
  private InputStream logIfNeeded(String title, InputStream content) {
    if (LOGGER.isDebugEnabled()) {
      String c;
      try {
        c = IOUtils.toString(content);
      } catch (IOException iox) {
        throw new RuntimeException(iox);
      }
      LOGGER.debug("Logging " + title);
      LOGGER.debug("-----------------------------------");
      LOGGER.debug(c);
      LOGGER.debug("-----------------------------------");
      return new ByteArrayInputStream(c.getBytes());
    }
    return content;
  }

  /**
   * Collects all the headers returned by the http call.
   */
  private Map<String, String> toHeadersMap(HttpResponse response) {
    return response.getHeaderNames().stream()
        .collect(toMap(identity(), name -> response.getHeaderValues(name).stream().collect(joining(" "))));
  }
}
