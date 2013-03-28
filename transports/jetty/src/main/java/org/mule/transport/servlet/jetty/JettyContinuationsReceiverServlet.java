/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.servlet.jetty;

import org.mule.DefaultMuleEvent;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.transport.MessageReceiver;
import org.mule.transport.http.HttpConnector;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.util.ajax.Continuation;
import org.mortbay.util.ajax.ContinuationSupport;

public class JettyContinuationsReceiverServlet extends JettyReceiverServlet
{
    // mutex used to make sure that the continuation is not resumed before it is suspended. 
    private Object mutex = new Object();
    
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        try
        {
            MuleMessage responseMessage = null;
            
            synchronized (mutex)
            {
                Continuation continuation = ContinuationSupport.getContinuation(request, mutex);
                
                if (!continuation.isPending())
                {
                    // case where we are processing this request for the first time (suspend has not been called)
                    
                    MessageReceiver receiver = getReceiverForURI(request);
                    
                    MuleMessage requestMessage = receiver.createMuleMessage(request);
                    requestMessage.setOutboundProperty(HttpConnector.HTTP_METHOD_PROPERTY, request.getMethod());
    
                    ContinuationsReplyTo continuationsReplyTo = new ContinuationsReplyTo(continuation, mutex);
                    //This will allow Mule to continue the response once the service has do its processing
                    requestMessage.setReplyTo(continuationsReplyTo);
                    setupRequestMessage(request, requestMessage, receiver);
                    
                    if (receiver instanceof JettyHttpMessageReceiver)
                    {
                        //we force asynchronous in the {@link #routeMessage} method
                        JettyHttpMessageReceiver jettyReceiver = (JettyHttpMessageReceiver) receiver;
                        jettyReceiver.routeMessageAsync(requestMessage, continuationsReplyTo);
                        
                        // suspend indefinitely
                        //TODO: perhaps we can make this configurable just like Http's keepSocketAlive is
                        continuation.suspend(0L);
                    }
                    else
                    {
                        responseMessage = receiver.routeMessage(requestMessage).getMessage();
                        writeResponse(response, responseMessage);
                    }
                }
                else
                {
                    // case where we are processing this request for the second time. 
                    if (continuation.isResumed())
                    {
                        // the continuation was resumed so the response should be there
                        Object r = continuation.getObject();
                        // response object is either a MuleMessage of an Exception if there was an error
                        if (r instanceof MuleMessage)
                        {
                            responseMessage = (MuleMessage) r;
                            // clear the object because jetty reuses continuations for the same connection
                            continuation.setObject(null);
                            
                            writeResponse(response, responseMessage);
                        }
                        else if (r instanceof Exception)
                        {
                            if (r instanceof MessagingException)
                            {
                                // Reset access control on the MuleEvent because its message's owner is a different thread
                                // Otherwise when the message is modified during exception handling, it will fail
                                MessagingException me = (MessagingException) r;
                                MuleEvent event = me.getEvent();
                                if (event instanceof DefaultMuleEvent)
                                {
                                    ((DefaultMuleEvent) event).resetAccessControl();
                                }
                            }
                            throw (Exception) r;
                        }
                    }
                }
            }
        }
        catch (RuntimeException e)
        {
            // Jetty continuations throw a subclass of RuntimeException when suspend is don't treat them as errors
            throw new ServletException(e);
        }
        catch (Exception e)
        {
            String message = e.getMessage();
            handleException(e, message, response);
        }
    }
}
