/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;

public interface SimpleServerSocketFactory 
{

    /**
     * @param uri The address and port to connect to
     * @param backlog The backlog (or {@link org.mule.umo.provider.UMOConnector#INT_VALUE_NOT_SET})
     * @param reuse Whether to reuse addresses (null for default)
     * @return A new, bound server socket
     * @throws IOException
     */
    ServerSocket createServerSocket(URI uri, int backlog, Boolean reuse) throws IOException;

    ServerSocket createServerSocket(InetAddress address, int port, int backlog, Boolean reuse) throws IOException;

    ServerSocket createServerSocket(int port, int backlog, Boolean reuse) throws IOException;

}
