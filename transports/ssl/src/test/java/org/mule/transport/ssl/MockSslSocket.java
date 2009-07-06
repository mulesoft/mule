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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

/**
 * {@link SSLSocket} subclass that can be used to mock SSL related tests
 */
public class MockSslSocket extends SSLSocket
{
    
    public void addHandshakeCompletedListener(HandshakeCompletedListener listener)
    {
        // not needed
    }

    public boolean getEnableSessionCreation()
    {
        return false;
    }

    public String[] getEnabledCipherSuites()
    {
        return null;
    }

    public String[] getEnabledProtocols()
    {
        return null;
    }

    public boolean getNeedClientAuth()
    {
        return false;
    }

    public SSLSession getSession()
    {
        return null;
    }

    public String[] getSupportedCipherSuites()
    {
        return null;
    }

    public String[] getSupportedProtocols()
    {
        return null;
    }

    public boolean getUseClientMode()
    {
        return false;
    }

    public boolean getWantClientAuth()
    {
        return false;
    }

    public void removeHandshakeCompletedListener(HandshakeCompletedListener listener)
    {
        // not needed
    }

    public void setEnableSessionCreation(boolean flag)
    {
        // not needed
    }

    public void setEnabledCipherSuites(String[] suites)
    {
        // not needed
    }

    public void setEnabledProtocols(String[] protocols)
    {
        // not needed
    }

    public void setNeedClientAuth(boolean need)
    {
        // not needed
    }

    public void setUseClientMode(boolean mode)
    {
        // not needed
    }

    public void setWantClientAuth(boolean want)
    {
        // not needed
    }

    public void startHandshake() throws IOException
    {
        // not needed
    }
    
    @Override
    public InputStream getInputStream() throws IOException
    {
        return null;
    }

    @Override
    public OutputStream getOutputStream() throws IOException
    {
        return null;
    }

    @Override
    public SocketAddress getRemoteSocketAddress()
    {
        return new InetSocketAddress("localhost", 12345);
    }

}

