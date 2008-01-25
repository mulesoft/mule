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

import org.mule.DefaultMuleMessage;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.service.Service;
import org.mule.api.transport.Connector;
import org.mule.transport.AbstractMessageReceiver;
import org.mule.transport.tcp.TcpMessageReceiver;

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

    public SslMessageReceiver(Connector connector, Service service, ImmutableEndpoint endpoint)
            throws CreateException
    {
        super(connector, service, endpoint);
    }

    protected Work createWork(Socket socket) throws IOException
    {
        return new SslWorker(socket, this);
    }

    private void preRoute(DefaultMuleMessage message) throws Exception
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

        protected void preRouteMuleMessage(DefaultMuleMessage message) throws Exception
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
