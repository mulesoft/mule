/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp;

import org.mule.config.i18n.Message;
import org.mule.providers.AbstractServiceEnabledConnector;
import org.mule.providers.tcp.protocols.DefaultProtocol;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.util.ClassUtils;

/**
 * <code>TcpConnector</code> can bind or sent to a given tcp port on a given host.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @author <a href="mailto:tsuppari@yahoo.co.uk">P.Oikari</a>
 * @version $Revision$
 */
public class TcpConnector extends AbstractServiceEnabledConnector
{
    public static final int DEFAULT_SOCKET_TIMEOUT = INT_VALUE_NOT_SET;

    public static final int DEFAULT_BUFFER_SIZE = INT_VALUE_NOT_SET;

    public static final int DEFAULT_BACKLOG = INT_VALUE_NOT_SET;

    protected int sendTimeout = DEFAULT_SOCKET_TIMEOUT;

    protected int receiveTimeout = DEFAULT_SOCKET_TIMEOUT;

    protected int bufferSize = DEFAULT_BUFFER_SIZE;

    protected int backlog = DEFAULT_BACKLOG;

    protected String tcpProtocolClassName = DefaultProtocol.class.getName();

    protected TcpProtocol tcpProtocol;

    protected boolean keepSendSocketOpen = false;

    protected boolean keepAlive = false;

    public boolean isKeepSendSocketOpen()
    {
        return keepSendSocketOpen;
    }

    public void doInitialise() throws InitialisationException
    {
        super.doInitialise();
        if (tcpProtocol == null)
        {
            try
            {
                tcpProtocol = (TcpProtocol)ClassUtils.instanciateClass(tcpProtocolClassName, null);
            }
            catch (Exception e)
            {
                throw new InitialisationException(new Message("tcp", 3), e);
            }
        }
    }

    public String getProtocol()
    {
        return "TCP";
    }

    /**
     * A shorthand property setting timeout for both SEND and RECEIVE sockets.
     */
    public void setTimeout(int timeout)
    {
        setSendTimeout(timeout);
        setReceiveTimeout(timeout);
    }

    public int getSendTimeout()
    {
        return this.sendTimeout;
    }

    public void setSendTimeout(int timeout)
    {
        if (timeout < 0)
        {
            timeout = DEFAULT_SOCKET_TIMEOUT;
        }
        this.sendTimeout = timeout;
    }

    // ////////////////////////////////////////////
    // New independednt Socket timeout for receiveSocket
    // ////////////////////////////////////////////
    public int getReceiveTimeout()
    {
        return receiveTimeout;
    }

    public void setReceiveTimeout(int timeout)
    {
        if (timeout < 0)
        {
            timeout = DEFAULT_SOCKET_TIMEOUT;
        }
        this.receiveTimeout = timeout;
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

    public int getBacklog()
    {
        return backlog;
    }

    public void setBacklog(int backlog)
    {
        this.backlog = backlog;
    }

    public TcpProtocol getTcpProtocol()
    {
        return tcpProtocol;
    }

    public void setTcpProtocol(TcpProtocol tcpProtocol)
    {
        this.tcpProtocol = tcpProtocol;
    }

    public String getTcpProtocolClassName()
    {
        return tcpProtocolClassName;
    }

    public void setTcpProtocolClassName(String protocolClassName)
    {
        this.tcpProtocolClassName = protocolClassName;
    }

    public boolean isRemoteSyncEnabled()
    {
        return true;
    }

    public boolean isKeepAlive()
    {
        return keepAlive;
    }

    public void setKeepAlive(boolean keepAlive)
    {
        this.keepAlive = keepAlive;
    }
}
