/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.ssl;


import java.security.cert.Certificate;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocket;

public class MockHandshakeCompletedEvent extends HandshakeCompletedEvent
{
    
    public MockHandshakeCompletedEvent()
    {
        this(new MockSslSocket());
    }
    
    public MockHandshakeCompletedEvent(SSLSocket socket)
    {
        super(socket, null);
    }

    @Override
    public Certificate[] getLocalCertificates()
    {
        return new Certificate[0];
    }

    @Override
    public Certificate[] getPeerCertificates() throws SSLPeerUnverifiedException
    {
        return new Certificate[0];
    }
    
}

