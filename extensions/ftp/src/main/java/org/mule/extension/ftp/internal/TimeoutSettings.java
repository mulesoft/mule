/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal;

import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED_TAB;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import java.util.concurrent.TimeUnit;

public final class TimeoutSettings {

  /**
   * A scalar value representing the amount of time to wait before a connection attempt times out. This attribute works in tandem
   * with {@link #connectionTimeoutUnit}.
   * <p>
   * Defaults to {@code 10}
   */
  @Parameter
  @Optional(defaultValue = "10")
  @Placement(tab = ADVANCED_TAB, order = 2)
  @Summary("Connection timeout value")
  private Integer connectionTimeout;

  /**
   * A {@link TimeUnit} which qualifies the {@link #connectionTimeout} attribute.
   * <p>
   * Defaults to {@code SECONDS}
   */
  @Parameter
  @Optional(defaultValue = "SECONDS")
  @Placement(tab = ADVANCED_TAB, order = 1)
  @Summary("Time unit to be used in the Connection Timeout")
  private TimeUnit connectionTimeoutUnit;

  /**
   * A scalar value representing the amount of time to wait before a request for data times out. This attribute works in tandem
   * with {@link #responseTimeoutUnit}.
   * <p>
   * Defaults to {@code 10}
   */
  @Parameter
  @Optional(defaultValue = "10")
  @Placement(tab = ADVANCED_TAB, order = 4)
  @Summary("Response timeout value")
  private Integer responseTimeout;

  /**
   * A {@link TimeUnit} which qualifies the {@link #responseTimeoutUnit} attribute.
   * <p>
   * Defaults to {@code SECONDS}
   */
  @Parameter
  @Optional(defaultValue = "SECONDS")
  @Placement(tab = ADVANCED_TAB, order = 3)
  @Summary("Time unit to be used in the Response Timeout")
  private TimeUnit responseTimeoutUnit;

  public Integer getConnectionTimeout() {
    return connectionTimeout;
  }

  public TimeUnit getConnectionTimeoutUnit() {
    return connectionTimeoutUnit;
  }

  public Integer getResponseTimeout() {
    return responseTimeout;
  }

  public TimeUnit getResponseTimeoutUnit() {
    return responseTimeoutUnit;
  }

  public void setConnectionTimeout(Integer connectionTimeout) {
    this.connectionTimeout = connectionTimeout;
  }

  public void setConnectionTimeoutUnit(TimeUnit connectionTimeoutUnit) {
    this.connectionTimeoutUnit = connectionTimeoutUnit;
  }

  public void setResponseTimeout(Integer responseTimeout) {
    this.responseTimeout = responseTimeout;
  }

  public void setResponseTimeoutUnit(TimeUnit responseTimeoutUnit) {
    this.responseTimeoutUnit = responseTimeoutUnit;
  }
}
