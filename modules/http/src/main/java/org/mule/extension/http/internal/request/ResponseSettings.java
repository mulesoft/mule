/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal.request;

import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED_TAB;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

/**
 * Groups parameters regarding how to generate responses
 *
 * @since 4.0
 */
public final class ResponseSettings {

  /**
   * By default, the response will be parsed (for example, a multipart response will be mapped as a Mule message with null payload
   * and inbound attachments with each part). If this property is set to false, no parsing will be done, and the payload will
   * always contain the raw contents of the HTTP response.
   */
  @Parameter
  @Optional(defaultValue = "true")
  @Placement(tab = ADVANCED_TAB, order = 1)
  @Summary("Indicates if the HTTP response should be parsed, or directly receive the raw content")
  private Boolean parseResponse;

  /**
   * Maximum time that the request element will block the execution of the flow waiting for the HTTP response. If this value is
   * not present, the default response timeout from the Mule configuration will be used.
   */
  @Parameter
  @Optional
  @Placement(tab = ADVANCED_TAB, order = 2)
  private Integer responseTimeout;

  public Boolean getParseResponse() {
    return parseResponse;
  }

  public Integer getResponseTimeout() {
    return responseTimeout;
  }
}
