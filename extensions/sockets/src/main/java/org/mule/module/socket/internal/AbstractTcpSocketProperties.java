/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.socket.internal;

import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.ConfigName;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.module.socket.api.TcpSocketProperties;

/**
 * Mutable base class for implementations of {@link TcpSocketProperties}
 *
 * @since 4.0
 */
public abstract class AbstractTcpSocketProperties implements TcpSocketProperties
{

    /**
     * The name of this config object, so that it can be referenced by config elements.
     */
    @ConfigName
    private String name;

    /**
     * If set, transmitted data is not collected together for greater efficiency but sent immediately.
     * <p>
     * Defaults to {@code true} even though Socket default is false because optimizing to reduce amount of network
     * traffic over latency is hardly ever a concern today.
     */
    @Parameter
    @Optional(defaultValue = "true")
    private Boolean sendTcpNoDelay = true;

    /**
     * This sets the SO_TIMEOUT value on client sockets. Reading from the socket will block for up to this long
     * (in milliseconds) before the read fails.
     * <p>
     * A value of 0 (the default) causes the read to wait indefinitely (if no data arrives).
     */
    @Parameter
    @Optional(defaultValue = "0")
    private Integer timeout = 0;

    /**
     * This sets the SO_LINGER value. This is related to how long (in milliseconds) the socket will take to close so
     * that any remaining data is transmitted correctly.
     * <p>
     * A value of -1 (default) disables linger on the socket.
     */
    @Parameter
    @Optional(defaultValue = "-1")
    private Integer linger = -1;

    // These options are undefined by default so that by default it is the OS TCP/IP stack that configures, or dynamically
    // manages, these values.
    /**
     * The size of the buffer (in bytes) used when sending data, set on the socket itself.
     */
    @Parameter
    @Optional
    private Integer sendBufferSize;

    /**
     * The size of the buffer (in bytes) used when receiving data, set on the socket itself.
     */
    @Parameter
    @Optional
    private Integer receiveBufferSize;

    /**
     * Enables SO_KEEPALIVE behavior on open sockets. This automatically checks socket connections that are open but
     * unused for long periods and closes them if the connection becomes unavailable.
     * <p>
     * This is a property on the socket itself and is used by a server socket to control whether connections to the
     * server are kept alive before they are recycled.
     */
    @Parameter
    @Optional
    private Boolean keepAlive;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    public Integer getSendBufferSize()
    {
        return sendBufferSize;
    }

    public void setSendBufferSize(Integer sendBufferSize)
    {
        this.sendBufferSize = sendBufferSize;
    }

    @Override
    public Integer getReceiveBufferSize()
    {
        return receiveBufferSize;
    }

    public void setReceiveBufferSize(Integer receiveBufferSize)
    {
        this.receiveBufferSize = receiveBufferSize;
    }

    @Override
    public Boolean getSendTcpNoDelay()
    {
        return sendTcpNoDelay;
    }

    public void setSendTcpNoDelay(Boolean sendTcpNoDelay)
    {
        this.sendTcpNoDelay = sendTcpNoDelay;
    }

    @Override
    public Integer getTimeout()
    {
        return timeout;
    }

    public void setTimeout(Integer timeout)
    {
        this.timeout = timeout;
    }

    @Override
    public Integer getLinger()
    {
        return linger;
    }

    public void setLinger(Integer linger)
    {
        this.linger = linger;
    }

    @Override
    public Boolean getKeepAlive()
    {
        return keepAlive;
    }

    public void setKeepAlive(Boolean keepAlive)
    {
        this.keepAlive = keepAlive;
    }
}
