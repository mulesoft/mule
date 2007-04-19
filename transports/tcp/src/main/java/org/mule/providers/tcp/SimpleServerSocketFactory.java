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

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;

public interface SimpleServerSocketFactory 
{

    ServerSocket createServerSocket(URI uri, int backlog) throws IOException;

    ServerSocket createServerSocket(int port, int backlog, InetAddress address) throws IOException;

    ServerSocket createServerSocket(int port, int backlog) throws IOException;

}
