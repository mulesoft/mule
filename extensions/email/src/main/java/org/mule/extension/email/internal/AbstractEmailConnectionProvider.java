/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal;

import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED;
import static org.mule.runtime.extension.api.annotation.param.display.Placement.GENERAL;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Generic contract for all email configurations.
 *
 * @since 4.0
 */
public abstract class AbstractEmailConnectionProvider<T> implements ConnectionProvider<T> {

  private static final String TIMEOUT_CONFIGURATION = "Timeout Configuration";
  /**
   * The socket read timeout value. This attribute works in tandem with {@link #timeoutUnit}.
   * <p>
   * Defaults to {@code 5}
   */
  @Parameter
  @Optional(defaultValue = "5")
  @Placement(tab = ADVANCED, group = TIMEOUT_CONFIGURATION, order = 3)
  @Summary("Socket read timeout")
  protected int readTimeout;

  /**
   * The socket write timeout value. This attribute works in tandem with {@link #timeoutUnit}.
   * <p>
   * Defaults to {@code 5}
   */
  @Parameter
  @Optional(defaultValue = "5")
  @Placement(tab = ADVANCED, group = TIMEOUT_CONFIGURATION, order = 4)
  @Summary("The socket write timeout value")
  protected int writeTimeout;

  /**
   * A {@link TimeUnit} which qualifies the {@link #connectionTimeout}, {@link #writeTimeout} and {@link #readTimeout} attributes.
   * <p>
   * Defaults to {@code SECONDS}
   */
  @Parameter
  @Optional(defaultValue = "SECONDS")
  @Placement(tab = ADVANCED, group = TIMEOUT_CONFIGURATION, order = 1)
  @Summary("Time unit to be used in the Timeout configurations")
  private TimeUnit timeoutUnit;

  /**
   * The socket connection timeout value. This attribute works in tandem with {@link #timeoutUnit}.
   * <p>
   * Defaults to {@code 5}
   */
  @Parameter
  @Optional(defaultValue = "5")
  @Placement(tab = ADVANCED, group = TIMEOUT_CONFIGURATION, order = 2)
  @Summary("Socket connection timeout value")
  private int connectionTimeout;

  /**
   * An additional custom set of properties to configure the connection session.
   */
  @Parameter
  @Optional
  @Placement(tab = ADVANCED, group = GENERAL)
  private Map<String, String> properties;

  /**
   * @return the additional custom properties to configure the session.
   */
  protected Map<String, String> getProperties() {
    return properties;
  }

  /**
   * @return the configured client socket connection timeout in milliseconds.
   */
  protected long getConnectionTimeout() {
    return timeoutUnit.toMillis(connectionTimeout);
  }

  /**
   * @return he configured client socket read timeout in milliseconds.
   */
  protected long getReadTimeout() {
    return timeoutUnit.toMillis(readTimeout);
  }

  /**
   * @return he configured client socket write timeout in milliseconds.
   */
  protected long getWriteTimeout() {
    return timeoutUnit.toMillis(writeTimeout);
  }
}
