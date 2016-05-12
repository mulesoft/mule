/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.tcp;

/**
 * Default mutable implementation of the {@code TcpClientSocketProperties} interface.
 */
public class DefaultTcpClientSocketProperties implements TcpClientSocketProperties
{
    // Use default value of 'true' even though Socket default is false because optimizing to reduce amount of network
    // traffic over latency is hardly ever a concern today.
    private static final boolean DEFAULT_SEND_TCP_NO_DELAY = true;
    private static final int DEFAULT_CONNECTION_TIMEOUT = 30000;

    private String name;

    private Boolean sendTcpNoDelay = DEFAULT_SEND_TCP_NO_DELAY;
    private Integer connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;

    // These options are undefined by default so that java.net.Socket defaults are used.  We do however document the
    // java.net.Socket defaults in the schema for usability.
    private Integer timeout;
    private Integer linger;

    // These options are undefined by default so that by default it is the OS TCP/IP stack that configures, or dynamically
    // manages, these values.
    private Integer sendBufferSize;
    private Integer receiveBufferSize;
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
    public Integer getConnectionTimeout()
    {
        return connectionTimeout;
    }

    public void setConnectionTimeout(Integer connectionTimeout)
    {
        this.connectionTimeout = connectionTimeout;
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
