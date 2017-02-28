/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal.request;

import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED_TAB;

import org.mule.extension.http.internal.HttpStreamingType;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

/**
 * Groups parameters which configure how a request is done
 *
 * @since 4.0
 */
public final class RequestSettings {

  /**
   * Specifies whether to follow redirects or not. Default value is true.
   */
  @Parameter
  @Optional(defaultValue = "true")
  @Placement(tab = ADVANCED_TAB, order = 1)
  private Boolean followRedirects;

  /**
   * Defines if the request should contain a body or not. If AUTO, it will depend on the method (GET, HEAD and OPTIONS will not
   * send a body).
   */
  @Parameter
  @Optional(defaultValue = "AUTO")
  @Placement(tab = ADVANCED_TAB, order = 2)
  private HttpSendBodyMode sendBodyMode;

  /**
   * Defines if the request should be sent using streaming or not. If this attribute is not present, the behavior will depend on
   * the type of the payload (it will stream only for InputStream). If set to true, it will always stream. If set to false, it
   * will never stream. As streaming is done the request will be sent user Transfer-Encoding: chunked.
   */
  @Parameter
  @Optional(defaultValue = "AUTO")
  @Placement(tab = ADVANCED_TAB, order = 3)
  @Summary("Defines if the request should be sent using streaming or not. If this attribute is not present, "
      + "the behavior will depend on the type of the payload (it will stream only for InputStream).")
  private HttpStreamingType requestStreamingMode;

  public Boolean getFollowRedirects() {
    return followRedirects;
  }

  public HttpSendBodyMode getSendBodyMode() {
    return sendBodyMode;
  }

  public HttpStreamingType getRequestStreamingMode() {
    return requestStreamingMode;
  }
}
