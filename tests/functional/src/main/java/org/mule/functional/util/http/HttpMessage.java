/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.util.http;

import static java.util.Collections.list;

import org.mule.runtime.api.util.MultiMap;
import org.mule.runtime.core.api.util.IOUtils;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

public class HttpMessage {

  private byte[] body = new byte[0];
  private String uri;
  private MultiMap queryParams;
  private Multimap<String, String> headers = ImmutableMultimap.<String, String>builder().build();

  public HttpMessage(HttpServletRequest request) throws IOException {
    this.body = IOUtils.toByteArray(request.getInputStream());
    ImmutableMultimap.Builder<String, String> headersBuilder = ImmutableMultimap.builder();
    Enumeration<String> headerNames = request.getHeaderNames();
    while (headerNames.hasMoreElements()) {
      String headerName = headerNames.nextElement();
      headersBuilder.putAll(headerName, list(request.getHeaders(headerName)));
    }
    this.headers = headersBuilder.build();
    uri = request.getRequestURI();
    queryParams = queryToMap(request.getQueryString());
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
