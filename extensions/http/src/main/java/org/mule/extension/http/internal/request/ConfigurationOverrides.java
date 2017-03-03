/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal.request;

import static org.mule.extension.http.internal.HttpConnectorConstants.CONFIGURATION_OVERRIDES;

import org.mule.extension.http.internal.HttpStreamingType;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

/**
 * Group which holds operation parameters which overrides config settings
 *
 * @since 4.0
 */
public final class ConfigurationOverrides {

  /**
   * Host where the requests will be sent.
   */
  @Parameter
  @Optional
  @Placement(tab = CONFIGURATION_OVERRIDES, order = 1)
  private String host;

  /**
   * Port where the requests will be sent.
   */
  @Parameter
  @Optional
  @Placement(tab = CONFIGURATION_OVERRIDES, order = 2)
  private Integer port;

  /**
   * Specifies whether to follow redirects or not.
   */
  @Parameter
  @Optional
  @Placement(tab = CONFIGURATION_OVERRIDES, order = 3)
  private Boolean followRedirects;

  /**
   * Defines if the request should contain a body or not.
   */
  @Parameter
  @Optional
  @Placement(tab = CONFIGURATION_OVERRIDES, order = 4)
  private HttpSendBodyMode sendBodyMode;

  /**
   * Defines if the request should be sent using streaming or not.
   */
  @Parameter
  @Optional
  @Placement(tab = CONFIGURATION_OVERRIDES, order = 5)
  private HttpStreamingType requestStreamingMode;

  /**
   * Defines if the HTTP response should be parsed or it's raw contents should be propagated instead.
   */
  @Parameter
  @Optional
  @Placement(tab = CONFIGURATION_OVERRIDES, order = 6)
  private Boolean parseResponse;

  /**
   * Maximum time that the request element will block the execution of the flow waiting for the HTTP response.
   */
  @Parameter
  @Optional
  @Placement(tab = CONFIGURATION_OVERRIDES, order = 7)
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
