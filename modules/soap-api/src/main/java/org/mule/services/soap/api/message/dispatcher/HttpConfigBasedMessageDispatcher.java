/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.api.message.dispatcher;

import static org.mule.runtime.api.metadata.DataType.INPUT_STREAM;
import static org.mule.runtime.extension.api.client.DefaultOperationParameters.builder;
import static org.mule.service.http.api.HttpConstants.Method.POST;
import static org.mule.service.http.api.HttpHeaders.Names.CONTENT_TYPE;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.extension.api.client.DefaultOperationParameters;
import org.mule.runtime.extension.api.client.ExtensionsClient;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.soap.message.DispatcherResponse;
import org.mule.runtime.extension.api.soap.message.DispatchingContext;
import org.mule.runtime.extension.api.soap.message.MessageDispatcher;
import org.mule.services.soap.api.exception.DispatchingException;

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
   * This method executes the request operation with the http {@code parseResponse} flag set to false in order to receive always
   * the complete response stream and to pass it to the service to process it.
   */
  @Override
  public DispatcherResponse dispatch(InputStream message, DispatchingContext context) {
    DefaultOperationParameters params = builder().configName(configName)
        .addParameter("method", POST.toString())
        .addParameter("url", context.getAddress())
        .addParameter("headers", context.getHeaders())
        .addParameter("parseResponse", false)
        .addParameter("body", new TypedValue<>(message, INPUT_STREAM))
        .build();
    try {
      Result<Object, Object> result = client.executeAsync("HTTP", "request", params).get();
      Map<String, List<String>> headers = getHttpHeaders(result);
      InputStream content = getContent(result);
      return new DispatcherResponse(getContentType(headers), content, headers);
    } catch (Exception e) {
      throw new DispatchingException("Could not dispatch soap message using the [" + configName + "] HTTP configuration", e);
    }
  }

  /**
   * Reflectively introspects the result to find the HTTP Headers.
   *
   * @param result the {@link Result} returned by the http request operation
   */
  private Map<String, List<String>> getHttpHeaders(Result<Object, Object> result) {
    try {
      Optional httpAttributes = result.getAttributes();
      if (!httpAttributes.isPresent()) {
        throw new IllegalStateException("No Http Attributes found on the response, cannot get response headers.");
      }
      Object headers = httpAttributes.get().getClass().getMethod("getHeaders").invoke(httpAttributes.get());
      return (Map<String, List<String>>) headers.getClass().getMethod("toListValuesMap").invoke(headers);
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

  /**
   * Returns the content type of the response by looking the Content-Type header.
   */
  private String getContentType(Map<String, List<String>> headers) {
    List<String> contentTypeValue = headers.get(CONTENT_TYPE.toLowerCase());
    if (contentTypeValue.size() == 1) {
      return contentTypeValue.get(0);
    }
    throw new IllegalStateException("Content-Type was expected to have one value but got [" + contentTypeValue.size() + "]");
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
