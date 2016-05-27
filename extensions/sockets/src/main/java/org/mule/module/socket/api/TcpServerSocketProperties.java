/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.socket.api;

/**
 * Interface for objects that provide TCP configuration for server sockets.
 * {@code null} values can be returned by any of the methods, meaning that there is no value defined for the property.
 *
 * @since 4.0
 */
public interface TcpServerSocketProperties extends TcpSocketProperties
{
    /**
     * The maximum queue length for incoming connections.
     */
    Integer getReceiveBacklog();

    /**
     * The maximum queue length for incoming connections.
     */
    Boolean getReuseAddress();

    /**
     * This sets the SO_TIMEOUT value when the socket is used as a server. This is the timeout that applies to the "accept" operation.
     * A value of 0 (the default) causes the accept to wait indefinitely (if no connection arrives).
     */
    Integer getServerTimeout();

}
