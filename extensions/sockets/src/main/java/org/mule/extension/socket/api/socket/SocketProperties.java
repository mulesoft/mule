/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.socket.api.socket;

/**
 * Interface for objects that provide common configuration for generic sockets.
 *
 * @since 4.0
 */
public interface SocketProperties {

  /**
   * The size of the buffer (in bytes) used when sending data, set on the socket itself.
   */
  Integer getSendBufferSize();

  /**
   * The size of the buffer (in bytes) used when receiving data, set on the socket itself.
   */
  Integer getReceiveBufferSize();

  /**
   * This sets the SO_TIMEOUT value on client sockets. Reading from the socket will block for up to this long (in milliseconds)
   * before the read fails.
   * <p>
   * A value of 0 (the socket's default) causes the read to wait indefinitely (if no data arrives).
   */
  Integer getClientTimeout();


  /**
   * Enabling SO_REUSEADDR prior to binding the socket using bind(SocketAddress) allows the socket to be bound even though a
   * previous connection is in a clientSocketTimeout state.
   * <p>
   * For UDP sockets it may be necessary to bind more than one socket to the same socket address. This is typically for the
   * purpose of receiving multicast packets
   */
  boolean getReuseAddress();
}
