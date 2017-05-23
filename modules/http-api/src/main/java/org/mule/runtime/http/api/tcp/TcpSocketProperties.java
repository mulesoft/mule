/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.tcp;

/**
 * Common TCP configuration that applies for both client and server sockets.
 *
 * @since 4.0
 */
public interface TcpSocketProperties {

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
   * If set, transmitted data is not collected together for greater efficiency but sent immediately.
   * <p>
   * Defaults to {@code true} even though Socket default is false because optimizing to reduce amount of network traffic over
   * latency is hardly ever a concern today.
   */
  Boolean getSendTcpNoDelay();

  /**
   * This sets the SO_LINGER value. This is related to how long (in milliseconds) the socket will take to close so that any
   * remaining data is transmitted correctly. Enabling this option with a non-zero Integer <I>clientSocketTimeout</I> means that a
   * <B>close()</B> will block pending the transmission and acknowledgement of all data written to the peer, at which point the
   * socket is closed <I>gracefully</I>. Upon reaching the linger clientSocketTimeout, the socket is closed <I>forcefully</I>,
   * with a TCP RST. Enabling the option with a clientSocketTimeout of zero does a forceful close immediately. If the specified
   * clientSocketTimeout value exceeds 65,535 it will be reduced to 65,535.
   * <p>
   * A value of {@code null} (default) disables linger on the socket.
   */
  Integer getLinger();

  /**
   * Enables SO_KEEPALIVE behavior on open sockets. This automatically checks socket connections that are open but unused for long
   * periods and closes them if the connection becomes unavailable.
   * <p>
   * This is a property on the socket itself and is used by a server socket to control whether connections to the server are kept
   * alive before they are recycled.
   */
  Boolean getKeepAlive();

}
