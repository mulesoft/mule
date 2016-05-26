/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.socket.api.socket;

import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.ConfigName;
import org.mule.runtime.extension.api.annotation.param.Optional;

/**
 * Configuration fields common to all socket implementations
 *
 * @since 4.0
 */
public abstract class AbstractSocketProperties implements SocketProperties
{

    /**
     * The name of this config object, so that it can be referenced by config elements.
     */
    @ConfigName
    protected String name;

    /**
     * The size of the buffer (in bytes) used when sending data, set on the socket itself.
     */
    @Parameter
    @Optional
    protected Integer sendBufferSize;

    /**
     * The size of the buffer (in bytes) used when receiving data, set on the socket itself.
     */
    @Parameter
    @Optional
    protected Integer receiveBufferSize;

    /**
     * This sets the SO_TIMEOUT value on sockets. Indicates the amount of time (in milliseconds)
     * that the socket will wait in a blocking operation before failing.
     * <p>
     * A value of 0 (the default) means waiting indefinitely.
     */
    @Parameter
    @Optional
    protected Integer clientTimeout;

    /**
     * If set (the default), SO_REUSEADDRESS is set on the sockets before binding.
     * This helps reduce "address already in use" errors when a socket is re-used.
     */
    @Parameter
    @Optional(defaultValue = "true")
    private boolean reuseAddress = true;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getSendBufferSize()
    {
        return sendBufferSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getReceiveBufferSize()
    {
        return receiveBufferSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getClientTimeout()
    {
        return clientTimeout;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getReuseAddress()
    {
        return reuseAddress;
    }
}
