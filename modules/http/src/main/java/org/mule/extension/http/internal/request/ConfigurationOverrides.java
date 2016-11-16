/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal.request;

import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED_TAB;
import org.mule.extension.http.api.HttpSendBodyMode;
import org.mule.extension.http.api.HttpStreamingType;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

public final class ConfigurationOverrides {

  /**
   * Host where the requests will be sent.
   */
  @Optional
  @Placement(tab = ADVANCED_TAB, order = 1)
  private String host;

  /**
   * Port where the requests will be sent.
   */
  @Optional
  @Placement(tab = ADVANCED_TAB, order = 2)
  private Integer port;

  /**
   * Specifies whether to follow redirects or not.
   */
  @Optional
  @Placement(tab = ADVANCED_TAB, order = 3)
  private Boolean followRedirects;

  /**
   * Defines if the request should contain a body or not.
   */
  @Optional
  @Placement(tab = ADVANCED_TAB, order = 4)
  private HttpSendBodyMode sendBodyMode;

  /**
   * Defines if the request should be sent using streaming or not.
   */
  @Optional
  @Placement(tab = ADVANCED_TAB, order = 5)
  private HttpStreamingType requestStreamingMode;

  /**
   * Defines if the HTTP response should be parsed or it's raw contents should be propagated instead.
   */
  @Optional
  @Placement(tab = ADVANCED_TAB, order = 6)
  private Boolean parseResponse;

  /**
   * Maximum time that the request element will block the execution of the flow waiting for the HTTP response.
   */
  @Optional
  @Placement(tab = ADVANCED_TAB, order = 7)
  private Integer responseTimeout;

  public String getHost() {
    return host;
  }

  public Integer getPort() {
    return port;
  }

  public Boolean getFollowRedirects() {
    return followRedirects;
  }

  public HttpSendBodyMode getSendBodyMode() {
    return sendBodyMode;
  }

  public HttpStreamingType getRequestStreamingMode() {
    return requestStreamingMode;
  }

  public Boolean getParseResponse() {
    return parseResponse;
  }

  public Integer getResponseTimeout() {
    return responseTimeout;
  }
}
