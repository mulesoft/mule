/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.tcp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;

public interface SimpleServerSocketFactory 
{

    /**
     * @param uri The address and port to connect to
     * @param backlog The backlog (or {@link org.mule.api.transport.Connector#INT_VALUE_NOT_SET})
     * @param reuse Whether to reuse addresses (null for default)
     * @return A new, bound server socket
     * @throws IOException
     */
    ServerSocket createServerSocket(URI uri, int backlog, Boolean reuse) throws IOException;

    ServerSocket createServerSocket(InetAddress address, int port, int backlog, Boolean reuse) throws IOException;

    ServerSocket createServerSocket(int port, int backlog, Boolean reuse) throws IOException;

}
