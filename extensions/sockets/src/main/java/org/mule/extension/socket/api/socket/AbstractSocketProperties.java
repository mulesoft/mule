/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket.api.socket;

import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.ConfigName;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

/**
 * Configuration fields common to all socket implementations
 *
 * @since 4.0
 */
public abstract class AbstractSocketProperties implements SocketProperties {

  private static final String BUFFER_CONFIGURATION = "Buffer Configuration";
  protected static final String TIMEOUT_CONFIGURATION = "Timeout Configuration";

  /**
   * The name of this config object, so that it can be referenced by config elements.
   */
  @ConfigName
  protected String name;

  /**
   * The size of the buffer (in bytes) used when sending data, set on the socket itself.
   */
  @Parameter
  @Optional
  @Placement(group = BUFFER_CONFIGURATION)
  protected Integer sendBufferSize;

  /**
   * The size of the buffer (in bytes) used when receiving data, set on the socket itself.
   */
  @Parameter
  @Optional
  @Placement(group = BUFFER_CONFIGURATION)
  protected Integer receiveBufferSize;

  /**
   * This sets the SO_TIMEOUT value on sockets. Indicates the amount of time (in milliseconds) that the socket will wait in a
   * blocking operation before failing.
   * <p>
   * A value of 0 (the default) means waiting indefinitely.
   */
  @Parameter
  @Optional
  @Summary("Time, in milliseconds, that the socket will wait in a blocking operation before failing")
  @Placement(group = TIMEOUT_CONFIGURATION)
  protected Integer clientTimeout;

  /**
   * If set (the default), SO_REUSEADDRESS is set on the sockets before binding. This helps reduce "address already in use" errors
   * when a socket is re-used.
   */
  @Parameter
  @Optional(defaultValue = "true")
  @Summary("Indicates whether if the configured socket could be reused or fail at when trying to bind it")
  private boolean reuseAddress = true;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Integer getSendBufferSize() {
    return sendBufferSize;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Integer getReceiveBufferSize() {
    return receiveBufferSize;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Integer getClientTimeout() {
    return clientTimeout;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean getReuseAddress() {
    return reuseAddress;
  }
}
