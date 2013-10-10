/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.servlet.jetty;

import org.mule.DefaultMuleEvent;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.transport.MessageReceiver;
import org.mule.api.transport.PropertyScope;
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
                    requestMessage.setProperty(HttpConnector.HTTP_METHOD_PROPERTY, request.getMethod(), PropertyScope.INBOUND);
    
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
