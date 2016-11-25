/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal;

import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED_TAB;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import java.util.concurrent.TimeUnit;

/**
 * Groups timeout related parameters
 *
 * @since 4.0
 */
public final class TimeoutSettings {

  /**
   * A {@link TimeUnit} which qualifies the {@link #connectionTimeout}, {@link #writeTimeout} and {@link #readTimeout} attributes.
   * <p>
   * Defaults to {@code SECONDS}
   */
  @Parameter
  @Optional(defaultValue = "SECONDS")
  @Placement(tab = ADVANCED_TAB, order = 1)
  @Summary("Time unit to be used in the Timeout configurations")
  private TimeUnit timeoutUnit;

  /**
   * The socket connection timeout value. This attribute works in tandem with {@link #timeoutUnit}.
   * <p>
   * Defaults to {@code 5}
   */
  @Parameter
  @Optional(defaultValue = "5")
  @Placement(tab = ADVANCED_TAB, order = 2)
  @Summary("Socket connection timeout value")
  private int connectionTimeout;

  /**
   * The socket read timeout value. This attribute works in tandem with {@link #timeoutUnit}.
   * <p>
   * Defaults to {@code 5}
   */
  @Parameter
  @Optional(defaultValue = "5")
  @Placement(tab = ADVANCED_TAB, order = 3)
  @Summary("Socket read timeout")
  private int readTimeout;

  /**
   * The socket write timeout value. This attribute works in tandem with {@link #timeoutUnit}.
   * <p>
   * Defaults to {@code 5}
   */
  @Parameter
  @Optional(defaultValue = "5")
  @Placement(tab = ADVANCED_TAB, order = 4)
  @Summary("The socket write timeout value")
  private int writeTimeout;

  public TimeUnit getTimeoutUnit() {
    return timeoutUnit;
  }

  public int getConnectionTimeout() {
    return connectionTimeout;
  }

  public int getReadTimeout() {
    return readTimeout;
  }

  public int getWriteTimeout() {
    return writeTimeout;
  }
}
