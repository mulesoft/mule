/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.tcp;

/**
 * TCP server specific configuration.
 *
 * @since 4.0
 */
public interface TcpServerSocketProperties extends TcpSocketProperties {

  /**
   * Enabling SO_REUSEADDR prior to binding the socket using bind(SocketAddress) allows the socket to be bound even though a
   * previous connection is in a clientSocketTimeout state.
   * <p>
   * For UDP sockets it may be necessary to bind more than one socket to the same socket address. This is typically for the
   * purpose of receiving multicast packets
   */
  Boolean getReuseAddress();

  /**
   * The maximum queue length for incoming connections.
   */
  Integer getReceiveBacklog();

  /**
   * Sets the SO_TIMEOUT value when the socket is used as a server. Reading from the socket will block for up to this long (in
   * milliseconds) before the read fails. A value of 0 causes the read to wait indefinitely (if no data arrives).
   */
  Integer getServerTimeout();

}
