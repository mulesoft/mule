/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.tcp;

import org.mule.config.i18n.Message;
import org.mule.providers.AbstractServiceEnabledConnector;
import org.mule.providers.tcp.protocols.DefaultProtocol;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.util.ClassHelper;

/**
 * <code>TcpConnector</code> can bind or sent to a given tcp port on a given
 * host.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class TcpConnector extends AbstractServiceEnabledConnector
{
    public static final int DEFAULT_SOCKET_TIMEOUT = 5000;

    public static final int DEFAULT_BUFFER_SIZE = 64 * 1024;

    public static final long DEFAULT_POLLING_FREQUENCY = 10;

    public static final int DEFAULT_BACKLOG = 256;

    private int timeout = DEFAULT_SOCKET_TIMEOUT;

    private int bufferSize = DEFAULT_BUFFER_SIZE;

    private int backlog = DEFAULT_BACKLOG;

    private String tcpProtocolClassName = DefaultProtocol.class.getName();

    private TcpProtocol tcpProtocol;

    public void doInitialise() throws InitialisationException
    {
        super.doInitialise();
        if (tcpProtocol == null) {
            try {
                tcpProtocol = (TcpProtocol) ClassHelper.instanciateClass(tcpProtocolClassName, null);
            } catch (Exception e) {
                throw new InitialisationException(new Message("tcp", 3), e);
            }
        }
    }

    public String getProtocol()
    {
        return "TCP";
    }

    public int getTimeout()
    {
        return timeout;
    }

    public void setTimeout(int timeout)
    {
        if (timeout < 0)
            timeout = DEFAULT_SOCKET_TIMEOUT;
        this.timeout = timeout;
    }

    public int getBufferSize()
    {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize)
    {
        if (bufferSize < 1)
            bufferSize = DEFAULT_BUFFER_SIZE;
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

    public boolean isRemoteSyncEnabled() {
        return true;
    }
}
