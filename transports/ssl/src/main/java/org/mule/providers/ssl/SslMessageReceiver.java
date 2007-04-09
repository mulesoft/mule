/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.ssl;

import org.mule.providers.AbstractConnector;
import org.mule.providers.tcp.TcpMessageReceiver;
import org.mule.umo.UMOComponent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

/**
 * <code>SslMessageReceiver</code> acts like a tcp server to receive socket
 * requests using SSL.
 *
 * TODO - TCP can be refactored to mirror the structure here, including the
 * ServerSocketFactory.  Once that is done, this class can be removed and the
 * TCP superclass used directly.
 */
public class SslMessageReceiver extends TcpMessageReceiver
{

    public SslMessageReceiver(AbstractConnector connector, UMOComponent component, UMOEndpoint endpoint)
        throws InitialisationException
    {
        super(connector, component, endpoint);
    }

    protected ServerSocket createSocket(URI uri)
        throws IOException, NoSuchAlgorithmException, KeyManagementException
    {
        return ((SslConnector) connector).getServerSocket(uri);
    }

}
