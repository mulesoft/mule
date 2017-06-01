/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.util.http;

import org.mule.runtime.core.api.util.IOUtils;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.sun.net.httpserver.HttpExchange;

import java.util.Set;

public class HttpMessage {

  private byte[] body = new byte[0];

  private Multimap<String, String> headers = ImmutableMultimap.<String, String>builder().build();

  public HttpMessage(HttpExchange httpExchange) {
    this.body = IOUtils.toByteArray(httpExchange.getRequestBody());
    ImmutableMultimap.Builder<String, String> headersBuilder = ImmutableMultimap.builder();
    Set<String> headerNames = httpExchange.getRequestHeaders().keySet();
    headerNames.stream()
        .forEach(headerName -> headersBuilder.putAll(headerName, httpExchange.getRequestHeaders().get(headerName)));
    this.headers = headersBuilder.build();
  }

  public byte[] getBody() {
    return body;
  }

  public Multimap<String, String> getHeaders() {
    return headers;
  }
}
