/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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

