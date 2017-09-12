/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.util.http;

import org.mule.runtime.api.util.MultiMap;
import org.mule.runtime.core.api.util.IOUtils;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.sun.net.httpserver.HttpExchange;

import java.util.Set;

public class HttpMessage {

  private byte[] body = new byte[0];
  private String uri;
  private MultiMap queryParams;
  private Multimap<String, String> headers = ImmutableMultimap.<String, String>builder().build();

  public HttpMessage(HttpExchange httpExchange) {
    this.body = IOUtils.toByteArray(httpExchange.getRequestBody());
    ImmutableMultimap.Builder<String, String> headersBuilder = ImmutableMultimap.builder();
    Set<String> headerNames = httpExchange.getRequestHeaders().keySet();
    headerNames.stream()
        .forEach(headerName -> headersBuilder.putAll(headerName, httpExchange.getRequestHeaders().get(headerName)));
    this.headers = headersBuilder.build();
    uri = httpExchange.getRequestURI().getPath();
    queryParams = queryToMap(httpExchange.getRequestURI().getQuery());
  }

  public byte[] getBody() {
    return body;
  }

  public Multimap<String, String> getHeaders() {
    return headers;
  }


  public MultiMap<String, String> getQueryParams() {
    return queryParams;
  }

  public String getUri() {
    return uri;
  }

  private MultiMap<String, String> queryToMap(String query) {
    MultiMap<String, String> result = new MultiMap();
    if (query == null) {
      return result;
    }
    for (String param : query.split("&")) {
      String pair[] = param.split("=");
      if (pair.length > 1) {
        result.put(pair[0], pair[1]);
      } else {
        result.put(pair[0], "");
      }
    }
    return result;
  }
}
