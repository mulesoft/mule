/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.ssl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.providers.tcp.TcpMessageDispatcher;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

/**
 * <code>TcpMessageDispatcher</code> will send transformed mule events over tcp.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public class SslMessageDispatcher extends TcpMessageDispatcher
{
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(SslMessageDispatcher.class);

    public SslMessageDispatcher(SslConnector connector)
    {
        super(connector);
    }

    protected Socket createSocket(int port, InetAddress inetAddress) throws IOException
    {
        SocketFactory factory = SSLSocketFactory.getDefault();
        SSLSocket socket = (SSLSocket)factory.createSocket(inetAddress, port);
        return socket;
    }
}
