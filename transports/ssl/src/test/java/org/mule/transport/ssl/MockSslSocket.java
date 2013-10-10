/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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

