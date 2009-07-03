/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http;

import org.mule.api.MessagingException;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.service.Service;
import org.mule.api.transport.Connector;
import org.mule.transport.http.i18n.HttpMessages;

import java.io.IOException;
import java.net.Socket;
import java.security.cert.Certificate;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocket;
import javax.resource.spi.work.Work;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

public class HttpsMessageReceiver extends HttpMessageReceiver
{

    public HttpsMessageReceiver(Connector connector, Service service, InboundEndpoint endpoint)
            throws CreateException
    {
        super(connector, service, endpoint);
    }

    @Override
    protected Work createWork(Socket socket) throws IOException
    {
        return new HttpsWorker(socket);
    }

    private class HttpsWorker extends HttpWorker implements HandshakeCompletedListener
    {
        private Certificate[] peerCertificateChain;
        private Certificate[] localCertificateChain;
        private final CountDownLatch latch = new CountDownLatch(1);

        public HttpsWorker(Socket socket) throws IOException
        {
            super(socket);
            ((SSLSocket) socket).addHandshakeCompletedListener(this);
        }

        @Override
        protected void preRouteMessage(MuleMessage message) throws MessagingException
        {
            try
            {
                long timeout = ((HttpsConnector) getConnector()).getSslHandshakeTimeout();
                boolean handshakeComplete = latch.await(timeout, TimeUnit.MILLISECONDS);
                if (!handshakeComplete)
                {
                    throw new MessagingException(HttpMessages.sslHandshakeDidNotComplete(), message);
                }
            }
            catch (InterruptedException e)
            {
                throw new MessagingException(HttpMessages.sslHandshakeDidNotComplete(),
                    message, e);
            }                   
        	
        	super.preRouteMessage(message);
            
            if (peerCertificateChain != null)
            {
                message.setProperty(HttpsConnector.PEER_CERTIFICATES, peerCertificateChain);
            }
            if (localCertificateChain != null)
            {
                message.setProperty(HttpsConnector.LOCAL_CERTIFICATES, localCertificateChain);
            }
        }

        public void handshakeCompleted(HandshakeCompletedEvent event)
        {
			try
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
			finally
			{
	            latch.countDown();
			}
        }
    }

}
