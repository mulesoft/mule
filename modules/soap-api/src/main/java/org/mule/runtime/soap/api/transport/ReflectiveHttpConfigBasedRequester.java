/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.soap.api.transport;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static org.mule.runtime.api.metadata.DataType.INPUT_STREAM;
import static org.mule.runtime.extension.api.client.DefaultOperationParameters.builder;
import static org.mule.runtime.http.api.HttpConstants.Method.GET;
import static org.mule.runtime.http.api.HttpConstants.Method.POST;

import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.api.util.MultiMap;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.extension.api.client.DefaultOperationParametersBuilder;
import org.mule.runtime.extension.api.client.ExtensionsClient;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.soap.api.exception.DispatchingException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Performs HTTP requests using an Http Connector configuration.
 * <p>
 * As the this lives in mule and it cannot depend on the HTTP extension, reflection is used to access the returned headers.
 *
 * @since 4.0
 */
public final class ReflectiveHttpConfigBasedRequester {

  private final String configName;
  private final ExtensionsClient client;

  public ReflectiveHttpConfigBasedRequester(String configName, ExtensionsClient client) {
    this.configName = configName;
    this.client = client;
  }

  /**
   * Performs a GET request to the URL passed as parameter with a set of headers.
   *
   * @param url     the URL to be requested
   * @param headers a set of headers that are going to be bounded to the request
   * @return a {@link Pair} in which the first element is the response and the second is a set of response headers.
   */
  public Pair<InputStream, Map<String, String>> get(String url, Map<String, String> headers) {
    return request(GET.toString(), url, headers, null);
  }

  /**
   * Performs a PORT request to the URL passed as parameter with a set of headers and a body content.
   *
   * @param url     the URL to be requested
   * @param headers a set of headers that are going to be bounded to the request
   * @param body    the content body bounded to the request.
   * @return a {@link Pair} in which the first element is the response and the second is a set of response headers.
   */
  public Pair<InputStream, Map<String, String>> post(String url, Map<String, String> headers, InputStream body) {
    return request(POST.toString(), url, headers, body);
  }

  private Pair<InputStream, Map<String, String>> request(String method,
                                                         String url,
                                                         Map<String, String> headers,
                                                         InputStream body) {
    DefaultOperationParametersBuilder params = builder().configName(configName)
        .addParameter("method", method)
        .addParameter("url", url)
        .addParameter("headers", new MultiMap<>(headers));

    if (body != null) {
      params.addParameter("body", new TypedValue<>(body, INPUT_STREAM));
    }

    try {
      Result<Object, Object> result = client.executeAsync("HTTP", "request", params.build()).get();
      Map<String, String> httpHeaders = getHttpHeaders(result);
      InputStream content = getContent(result);
      return new Pair<>(content, httpHeaders);
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
}
