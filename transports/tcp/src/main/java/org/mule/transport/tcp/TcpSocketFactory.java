/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class TcpSocketFactory extends AbstractTcpSocketFactory
{

    protected Socket createSocket(TcpSocketKey key) throws IOException
    {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(key.getInetAddress(), key.getPort()), key.getEndpoint().getResponseTimeout());
        return socket;
    }

}
