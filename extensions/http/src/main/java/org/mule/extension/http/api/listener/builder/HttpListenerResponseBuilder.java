/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api.listener.builder;

import org.mule.extension.http.api.HttpMessageBuilder;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.extension.api.annotation.dsl.xml.XmlHints;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

/**
 * Base class for a {@link HttpMessageBuilder} which returns errors responses
 *
 * @since 4.0
 */
public abstract class HttpListenerResponseBuilder extends HttpMessageBuilder {

  /**
   * The body of the response message
   */
  @Parameter
  @Optional(defaultValue = "#[payload]")
  @XmlHints(allowReferences = false)
  private Object body;

  /**
   * HTTP status code the response should have.
   */
  @Parameter
  @Optional
  private Integer statusCode;

  /**
   * HTTP reason phrase the response should have.
   */
  @Parameter
  @Optional
  private String reasonPhrase;

  public Object getBody() {
    return body;
  }

  public void setBody(Object body) {
    this.body = body;
  }

  public abstract MediaType getMediaType();

  public Integer getStatusCode() {
    return statusCode;
  }

  public String getReasonPhrase() {
    return reasonPhrase;
  }
}
