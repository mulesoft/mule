/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.domain.message.response;

import static org.mule.runtime.http.api.server.HttpServerProperties.PRESERVE_HEADER_CASE;

import org.mule.runtime.http.api.domain.CaseInsensitiveMultiMap;
import org.mule.runtime.http.api.domain.message.HttpMessageBuilder;

/**
 * Builder of {@link HttpResponse}s. Instances can only be obtained using {@link HttpResponse#builder()}.
 * By default, the status code is 200 with empty reason phrase, headers and entity.
 *
 * @since 4.0
 */
public final class HttpResponseBuilder extends HttpMessageBuilder<HttpResponseBuilder, HttpResponse> {

  HttpResponseBuilder() {}

  private ResponseStatus responseStatus = new ResponseStatus();

  /**
   * Instantiates a builder that has as starting point another {@link HttpResponse}
   *
   * @param httpResponse the {@link HttpResponse} to configure this builder with.
   */
  public HttpResponseBuilder(HttpResponse httpResponse) {
    super(httpResponse);
    responseStatus(httpResponse);
  }

  @Override
  protected void initHeaders() {
    headers = new CaseInsensitiveMultiMap(!PRESERVE_HEADER_CASE);
  }

  private void responseStatus(HttpResponse httpResponse) {
    statusCode(httpResponse.getStatusCode());
    reasonPhrase(httpResponse.getReasonPhrase());
  }

  /**
   * @param statusCode the HTTP status line code desired for the {@link HttpResponse}
   * @return this builder
   */
  public HttpResponseBuilder statusCode(Integer statusCode) {
    this.responseStatus.setStatusCode(statusCode);
    return this;
  }

  /**
   * @param reasonPhrase the HTTP status line reason phrase desired for the {@link HttpResponse}
   * @return
   */
  public HttpResponseBuilder reasonPhrase(String reasonPhrase) {
    this.responseStatus.setReasonPhrase(reasonPhrase);
    return this;
  }

  /**
   * @return the current status code configured in the builder.
   */
  public int getStatusCode() {
    return responseStatus.getStatusCode();
  }

  /**
   * @return the current reason phrase configured in the builder.
   */
  public String getReasonPhrase() {
    return responseStatus.getReasonPhrase();
  }

  /**
   * Discard this builder after calling this method.
   * 
   * @return an {@link HttpResponse} as described
   */
  @Override
  public HttpResponse build() {
    return new DefaultHttpResponse(responseStatus, headers, entity);
  }

}
