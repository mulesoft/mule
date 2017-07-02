/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.soap.api.message.dispatcher;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static org.mule.runtime.api.metadata.DataType.INPUT_STREAM;
import static org.mule.runtime.extension.api.client.DefaultOperationParameters.builder;
import static org.mule.runtime.http.api.HttpConstants.Method.POST;
import static org.mule.runtime.http.api.HttpHeaders.Names.CONTENT_TYPE;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.extension.api.client.DefaultOperationParameters;
import org.mule.runtime.extension.api.client.ExtensionsClient;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.soap.message.DispatchingResponse;
import org.mule.runtime.extension.api.soap.message.DispatchingRequest;
import org.mule.runtime.extension.api.soap.message.MessageDispatcher;
import org.mule.runtime.soap.api.exception.DispatchingException;

import com.google.common.collect.ImmutableMap;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A {@link MessageDispatcher} that dispatches the SOAP request via HTTP using an HTTP connector provided configuration.
 * <p>
 * As the this lives in mule and it cannot depend on the HTTP extension, reflection is used to access the returned headers.
 *
 * @since 4.0
 */
public final class HttpConfigBasedMessageDispatcher implements MessageDispatcher {

  private final String configName;
  private final ExtensionsClient client;

  public HttpConfigBasedMessageDispatcher(String configName, ExtensionsClient client) {
    this.configName = configName;
    this.client = client;
  }

  /**
   * {@inheritDoc}
   * <p>
   * Dispatches the message using the {@link ExtensionsClient} executing the {@code request} operation of the HTTP extension.
   * <p>
   */
  @Override
  public DispatchingResponse dispatch(DispatchingRequest context) {
    ImmutableMap<Object, Object> headers = ImmutableMap.builder()
        .putAll(context.getHeaders())
        // It's important that content type is bundled with the headers
        .put(CONTENT_TYPE, context.getContentType())
        .build();

    DefaultOperationParameters params = builder().configName(configName)
        .addParameter("method", POST.toString())
        .addParameter("url", context.getAddress())
        .addParameter("headers", headers)
        .addParameter("parseResponse", false)
        .addParameter("body", new TypedValue<>(context.getContent(), INPUT_STREAM))
        .build();
    try {
      Result<Object, Object> result = client.executeAsync("HTTP", "request", params).get();
      Map<String, String> httpHeaders = getHttpHeaders(result);
      InputStream content = getContent(result);
      return new DispatchingResponse(content, httpHeaders.get(CONTENT_TYPE.toLowerCase()), httpHeaders);
    } catch (Exception e) {
      throw new DispatchingException("Could not dispatch soap message using the [" + configName + "] HTTP configuration", e);
    }
  }

  /**
   * Reflectively introspects the result to find the HTTP Headers.
   *
   * @param result the {@link Result} returned by the http request operation
   */
  private Map<String, String> getHttpHeaders(Result<Object, Object> result) {
    try {
      Optional httpAttributes = result.getAttributes();
      if (!httpAttributes.isPresent()) {
        throw new IllegalStateException("No Http Attributes found on the response, cannot get response headers.");
      }
      Object headers = httpAttributes.get().getClass().getMethod("getHeaders").invoke(httpAttributes.get());
      Map<String, List<String>> map = (Map<String, List<String>>) headers.getClass().getMethod("toListValuesMap").invoke(headers);
      return map.entrySet().stream().collect(toMap(e -> e.getKey(), e -> e.getValue().stream().collect(joining(" "))));
    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
      throw new IllegalStateException("Something went wrong when introspecting the http response attributes.", e);
    }
  }

  /**
   * Retrieves the content as an input stream validating it.
   */
  private InputStream getContent(Result<Object, Object> result) {
    Object output = result.getOutput();
    if (output instanceof CursorStreamProvider) {
      return ((CursorStreamProvider) output).openCursor();
    } else if (output instanceof InputStream) {
      return (InputStream) output;
    }
    throw new IllegalStateException("Content was expected to be an stream but got a [" + output.getClass().getName() + "]");
  }

  @Override
  public void dispose() {
    // do nothing
  }

  @Override
  public void initialise() throws InitialisationException {
    // do nothing
  }
}
