/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.client.remoting;

import org.mule.api.config.MuleProperties;
import org.mule.module.client.RemoteDispatcher;

import java.io.Serializable;

/**
 * The message type used for the handshake between the client {@link RemoteDispatcher} and the server
 * {@link RemoteDispatcherAgent}.
 *
 * <b>Deprecated as of 3.6.0</b>
 */
@Deprecated
public class ServerHandshake implements Serializable
{
    public static final String SERVER_HANDSHAKE_PROPERTY = MuleProperties.PROPERTY_PREFIX + "CLIENT_HANDSHAKE";

    private String wireFormatClass;

    public String getWireFormatClass()
    {
        return wireFormatClass;
    }

    public void setWireFormatClass(String wireFormatClass)
    {
        this.wireFormatClass = wireFormatClass;
    }
}
