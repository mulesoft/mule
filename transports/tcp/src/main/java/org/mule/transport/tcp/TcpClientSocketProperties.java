/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.tcp;

/**
 * Interface for objects that provide TCP configuration for client sockets.
 * Null values can be returned by any of the methods, meaning that there is no value defined for the property.
 */
public interface TcpClientSocketProperties
{

    public Integer getSendBufferSize();

    public Integer getReceiveBufferSize();

    public Boolean getSendTcpNoDelay();

    public Integer getConnectionTimeout();

    public Integer getTimeout();

    public Integer getLinger();

    public Boolean getKeepAlive();

}
