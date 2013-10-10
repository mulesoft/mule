/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.client.remoting;

import org.mule.api.config.MuleProperties;
import org.mule.module.client.RemoteDispatcher;

import java.io.Serializable;

/**
 * The message type used for the handshake between the client {@link RemoteDispatcher} and the server
 * {@link RemoteDispatcherAgent}.
 */
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
