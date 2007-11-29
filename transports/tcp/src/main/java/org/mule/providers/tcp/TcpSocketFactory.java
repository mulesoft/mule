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
import java.net.Socket;

public class TcpSocketFactory extends AbstractTcpSocketFactory
{

    protected Socket createSocket(TcpSocketKey key) throws IOException
    {
        return new Socket(key.getInetAddress(), key.getPort());
    }

}
