/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.socket.api;

/**
 * Interface for objects that provide common TCP configuration that applies for both
 * client and server sockets.
 * <p>
 * {@code null} values can be returned by any of the methods, meaning that there is no value defined for the property.
 *
 * @since 4.0
 */
public interface TcpSocketProperties
{

    /**
     * The size of the buffer (in bytes) used when sending data, set on the socket itself.
     */
    Integer getSendBufferSize();

    /**
     * The size of the buffer (in bytes) used when receiving data, set on the socket itself.
     */
    Integer getReceiveBufferSize();

    /**
     * If set, transmitted data is not collected together for greater efficiency but sent immediately.
     * <p>
     * Defaults to {@code true} even though Socket default is false because optimizing to reduce amount of network
     * traffic over latency is hardly ever a concern today.
     */
    Boolean getSendTcpNoDelay();

    /**
     * This sets the SO_TIMEOUT value on client sockets. Reading from the socket will block for up to this long
     * (in milliseconds) before the read fails.
     * <p>
     * A value of 0 (the default) causes the read to wait indefinitely (if no data arrives).
     */
    Integer getTimeout();

    /**
     * This sets the SO_LINGER value. This is related to how long (in milliseconds) the socket will take to close so
     * that any remaining data is transmitted correctly.
     * <p>
     * A value of -1 (default) disables linger on the socket.
     */
    Integer getLinger();

    /**
     * Enables SO_KEEPALIVE behavior on open sockets. This automatically checks socket connections that are open but
     * unused for long periods and closes them if the connection becomes unavailable.
     * <p>
     * This is a property on the socket itself and is used by a server socket to control whether connections to the
     * server are kept alive before they are recycled.
     */
    Boolean getKeepAlive();

}
