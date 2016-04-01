/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.socket.api;

/**
 * Interface for objects that provide TCP configuration for client sockets.
 * {@code null} values can be returned by any of the methods, meaning that there is no value defined for the property.
 *
 * @since 4.0
 */
public interface TcpClientSocketProperties extends TcpSocketProperties
{

    /**
     * Number of milliseconds to wait until an outbound connection to a remote server is successfully created.
     * Defaults to 30 seconds.
     */
    Integer getConnectionTimeout();
}
