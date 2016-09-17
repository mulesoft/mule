/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api.listener.builder;

import org.mule.extension.http.api.HttpMessageBuilder;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.dsl.xml.XmlHints;
import org.mule.runtime.extension.api.annotation.param.Optional;

import java.util.Map;
import java.util.function.Function;

/**
 * Component that specifies how to create a proper HTTP response.
 *
 * @since 4.0
 */
@Alias("response-builder")
@XmlHints(allowTopLevelDefinition = true)
public class HttpListenerResponseBuilder extends HttpMessageBuilder {

  /**
   * HTTP status code the response should have.
   */
  @Parameter
  @Optional
  private Function<Event, Integer> statusCode;

  /**
   * HTTP reason phrase the response should have.
   */
  @Parameter
  @Optional
  private Function<Event, String> reasonPhrase;

  /**
   * HTTP headers the response should have, as an expression. Will override the headers attribute.
   */
  @Parameter
  @Optional
  private Function<Event, Map> headersRef;

  public Integer getStatusCode(Event event) {
    return statusCode != null ? statusCode.apply(event) : null;
  }

  public String getReasonPhrase(Event event) {
    return reasonPhrase != null ? reasonPhrase.apply(event) : null;
  }

  public Map<String, String> getHeaders(Event event) {
    return headersRef != null ? headersRef.apply(event) : headers;
  }

}
