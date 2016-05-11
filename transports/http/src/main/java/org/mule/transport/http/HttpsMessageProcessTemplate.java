/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.WorkManager;
import org.mule.transport.http.i18n.HttpMessages;

import java.util.concurrent.TimeUnit;

public class HttpsMessageProcessTemplate extends HttpMessageProcessTemplate
{

    public HttpsMessageProcessTemplate(final HttpMessageReceiver messageReceiver, final HttpServerConnection httpServerConnection, final WorkManager flowExecutionWorkManager)
    {
        super(messageReceiver,httpServerConnection);
    }

    @Override
    public MuleEvent beforeRouteEvent(MuleEvent muleEvent) throws MuleException
    {
        try
        {
            long timeout = ((HttpsConnector) getConnector()).getSslHandshakeTimeout();
            boolean handshakeComplete = getHttpServerConnection().getSslSocketHandshakeCompleteLatch().await(timeout, TimeUnit.MILLISECONDS);
            if (!handshakeComplete)
            {
                throw new MessagingException(HttpMessages.sslHandshakeDidNotComplete(), muleEvent);
            }
        }
        catch (InterruptedException e)
        {
            throw new MessagingException(HttpMessages.sslHandshakeDidNotComplete(),
                                         muleEvent, e);
        }
        if (getHttpServerConnection().getPeerCertificateChain() != null)
        {
            muleEvent.getMessage().setOutboundProperty(HttpsConnector.PEER_CERTIFICATES, getHttpServerConnection().getPeerCertificateChain());
        }
        if (getHttpServerConnection().getLocalCertificateChain() != null)
        {
            muleEvent.getMessage().setOutboundProperty(HttpsConnector.LOCAL_CERTIFICATES, getHttpServerConnection().getLocalCertificateChain());
        }

        super.beforeRouteEvent(muleEvent);
        return muleEvent;
    }
}
