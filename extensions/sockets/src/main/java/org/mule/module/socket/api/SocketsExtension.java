/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.socket.api;

import org.mule.module.socket.internal.DefaultTcpClientSocketProperties;
import org.mule.module.socket.internal.DefaultTcpServerSocketProperties;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.SubTypeMapping;
import org.mule.runtime.extension.api.annotation.param.Optional;

/**
 * A simple extension which allows configuring sockets
 *
 * @since 4.0
 */
@Extension(name = "sockets")
@SubTypeMapping(baseType = TcpServerSocketProperties.class, subTypes = DefaultTcpServerSocketProperties.class)
@SubTypeMapping(baseType = TcpClientSocketProperties.class, subTypes = DefaultTcpClientSocketProperties.class)
public class SocketsExtension
{

    /**
     * Provides TCP configuration for client sockets.
     */
    @Parameter
    @Optional
    private TcpClientSocketProperties tcpClientSocketProperties;

    /**
     * Provides TCP configuration for server sockets.
     */
    @Parameter
    @Optional
    private TcpServerSocketProperties tcpServerSocketProperties;

    public TcpClientSocketProperties getTcpClientSocketProperties()
    {
        return tcpClientSocketProperties;
    }

    public TcpServerSocketProperties getTcpServerSocketProperties()
    {
        return tcpServerSocketProperties;
    }
}
