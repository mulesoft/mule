/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.socket.internal;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.module.socket.api.TcpServerSocketProperties;

/**
 * Default mutable implementation of the {@code TcpServerSocketProperties} interface.
 *
 * @since 4.0
 */
@Alias("server-socket-properties")
public class DefaultTcpServerSocketProperties extends AbstractTcpSocketProperties implements TcpServerSocketProperties
{

    /**
     * If set (the default), SO_REUSEADDRESS is set on server sockets before binding.
     * This helps reduce "address already in use" errors when a socket is re-used.
     */
    @Parameter
    @Optional(defaultValue = "true")
    private Boolean reuseAddress = true;

    /**
     * This sets the SO_TIMEOUT value when the socket is used as a server. This is the timeout that applies to the "accept" operation.
     * A value of 0 (the default) causes the accept to wait indefinitely (if no connection arrives).
     */
    // These options are undefined by default so that java.net.Socket/java.net.ServerSocket defaults are used.  We do
    // however document the java.net.Socket/java.net.ServerSocket defaults in the schema for usability.
    @Parameter
    @Optional(defaultValue = "0")
    private Integer serverTimeout = 0;

    /**
     * The maximum queue length for incoming connections.
     */
    @Parameter
    @Optional
    private Integer receiveBacklog;

    @Override
    public Integer getReceiveBacklog()
    {
        return receiveBacklog;
    }

    public void setReceiveBacklog(Integer receiveBacklog)
    {
        this.receiveBacklog = receiveBacklog;
    }

    @Override
    public Boolean getReuseAddress()
    {
        return reuseAddress;
    }

    public void setReuseAddress(Boolean reuseAddress)
    {
        this.reuseAddress = reuseAddress;
    }

    @Override
    public Integer getServerTimeout()
    {
        return serverTimeout;
    }

    public void setServerTimeout(Integer serverTimeout)
    {
        this.serverTimeout = serverTimeout;
    }
}
