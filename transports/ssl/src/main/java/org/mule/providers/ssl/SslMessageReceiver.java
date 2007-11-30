/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.ssl;

import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageReceiver;
import org.mule.providers.tcp.TcpMessageReceiver;
import org.mule.umo.UMOComponent;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.CreateException;
import org.mule.umo.provider.UMOConnector;

import java.io.IOException;
import java.net.Socket;
import java.security.cert.Certificate;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocket;
import javax.resource.spi.work.Work;


public class SslMessageReceiver extends TcpMessageReceiver implements HandshakeCompletedListener
{
    private Certificate[] peerCertificateChain;
    private Certificate[] localCertificateChain;

    public SslMessageReceiver(UMOConnector connector, UMOComponent component, UMOImmutableEndpoint endpoint)
            throws CreateException
    {
        super(connector, component, endpoint);
    }

    protected Work createWork(Socket socket) throws IOException
    {
        return new SslWorker(socket, this);
    }

    private void preRoute(MuleMessage message) throws Exception
    {
        if(peerCertificateChain != null) message.setProperty(SslConnector.PEER_CERTIFICATES, peerCertificateChain);
        if(localCertificateChain != null) message.setProperty(SslConnector.LOCAL_CERTIFICATES, localCertificateChain);
    }

    public void handshakeCompleted(HandshakeCompletedEvent event)
    {
        localCertificateChain = event.getLocalCertificates();
        try
        {
            peerCertificateChain = event.getPeerCertificates();
        }
        catch (SSLPeerUnverifiedException e)
        {
            logger.debug("Cannot get peer certificate chain: "+ e.getMessage());
        }
    }

    protected class SslWorker extends TcpWorker
    {
        public SslWorker(Socket socket, AbstractMessageReceiver receiver) throws IOException
        {
            super(socket, receiver);
            ((SSLSocket) socket).addHandshakeCompletedListener(SslMessageReceiver.this);
        }

        protected void preRouteMuleMessage(MuleMessage message) throws Exception
        {
            super.preRouteMuleMessage(message);

            preRoute(message);
        }

        protected void shutdownSocket() throws IOException
        {
            // SSL Sockets don't support shutdownSocket
        }
    }
}
