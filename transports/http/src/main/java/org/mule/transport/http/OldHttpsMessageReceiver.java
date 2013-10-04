/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http;

import org.mule.api.MessagingException;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.transport.Connector;
import org.mule.config.i18n.CoreMessages;
import org.mule.transport.ConnectException;
import org.mule.transport.http.i18n.HttpMessages;
import org.mule.util.StringUtils;

import java.io.IOException;
import java.net.Socket;
import java.security.cert.Certificate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocket;
import javax.resource.spi.work.Work;

public class OldHttpsMessageReceiver extends OldHttpMessageReceiver
{
    public OldHttpsMessageReceiver(Connector connector, FlowConstruct flow, InboundEndpoint endpoint)
            throws CreateException
    {
        super(connector, flow, endpoint);
    }

    @Override
    protected void doConnect() throws ConnectException
    {
        checkKeyStore();
        super.doConnect();
    }

    protected void checkKeyStore() throws ConnectException
    {
        HttpsConnector httpsConnector = (HttpsConnector) connector;
        String keyStore = httpsConnector.getKeyStore();
        if (StringUtils.isBlank(keyStore))
        {
            throw new ConnectException(CoreMessages.objectIsNull("tls-key-store"), this);
        }
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
                message.setOutboundProperty(HttpsConnector.PEER_CERTIFICATES, peerCertificateChain);
            }
            if (localCertificateChain != null)
            {
                message.setOutboundProperty(HttpsConnector.LOCAL_CERTIFICATES, localCertificateChain);
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

