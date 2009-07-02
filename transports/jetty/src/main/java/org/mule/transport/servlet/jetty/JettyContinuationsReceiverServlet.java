/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.servlet.jetty;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.transport.MessageReceiver;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.servlet.HttpRequestMessageAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.util.ajax.Continuation;
import org.mortbay.util.ajax.ContinuationSupport;

public class JettyContinuationsReceiverServlet extends JettyReceiverServlet
{
    private Object mutex = new Object();

    @Override
    protected MuleMessage doMethod(HttpServletRequest request, HttpServletResponse response, String method) throws MuleException
    {
        final Continuation continuation = ContinuationSupport.getContinuation(request, mutex);
        synchronized (mutex)
        {
            MessageReceiver receiver = getReceiverForURI(request);

            MuleMessage requestMessage = new DefaultMuleMessage(new HttpRequestMessageAdapter(request), muleContext);
            requestMessage.setProperty(HttpConnector.HTTP_METHOD_PROPERTY, method);
            //Need to remove this if set, we'll be returning a result but we need to make the request async
            requestMessage.removeProperty(MuleProperties.MULE_REMOTE_SYNC_PROPERTY);
            //This will allow Mule to continue the response once the service has do its processing
            requestMessage.setReplyTo(continuation);
            setupRequestMessage(request, requestMessage, receiver);

            //we force asynchronous in the {@link #routeMessage} method
            routeMessage(receiver, requestMessage, request);

            continuation.suspend(10000);
        }

        return (MuleMessage)continuation.getObject();
    }

    protected MuleMessage routeMessage(MessageReceiver receiver, MuleMessage requestMessage, HttpServletRequest request)
        throws MuleException
    {
        //Force asynchronous processing since we are using continuations
        return receiver.routeMessage(requestMessage, false);
    }
}