/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.udp;

import org.mule.providers.AbstractConnector;
import org.mule.umo.UMOException;
import org.mule.umo.lifecycle.InitialisationException;

/**
 * <code>UdpConnector</code> can send and receive Mule events as Datagram packets.
 */
public class UdpConnector extends AbstractConnector
{
    public static final int DEFAULT_SOCKET_TIMEOUT = 5000;
    public static final int DEFAULT_BUFFER_SIZE = 64 * 1024;

    private int timeout = DEFAULT_SOCKET_TIMEOUT;
    private int bufferSize = DEFAULT_BUFFER_SIZE;


    protected void doInitialise() throws InitialisationException
    {
        // template method, nothing to do
    }

    protected void doDispose()
    {
        // template method
    }

    protected void doConnect() throws Exception
    {
        // template method
    }

    protected void doDisconnect() throws Exception
    {
        // template method
    }

    protected void doStart() throws UMOException
    {
        // template method
    }

    protected void doStop() throws UMOException
    {
        // template method
    }

    public String getProtocol()
    {
        return "UDP";
    }

    public int getTimeout()
    {
        return timeout;
    }

    public void setTimeout(int timeout)
    {
        if (timeout < 1)
        {
            timeout = DEFAULT_SOCKET_TIMEOUT;
        }
        this.timeout = timeout;
    }

    public int getBufferSize()
    {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize)
    {
        if (bufferSize < 1)
        {
            bufferSize = DEFAULT_BUFFER_SIZE;
        }
        this.bufferSize = bufferSize;
    }

}
