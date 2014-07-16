/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet.jetty;

import org.mule.api.MuleContext;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationSupport;

public class JettyContinuationsReceiverServlet extends JettyReceiverServlet
{

    public static final String RESPONSE_HANDLER_KEY = "RESPONSE_HANDLER";

    public JettyContinuationsReceiverServlet(MuleContext muleContext)
    {
        super(muleContext);
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        try
        {

            Continuation continuation = ContinuationSupport.getContinuation(request);

            if (continuation.isInitial())
            {
                // case where we are processing this request for the first time (suspend has not been called)
                final JettyHttpMessageReceiver receiver = (JettyHttpMessageReceiver) getReceiverForURI(request);
                final JettyHttpMessageReceiver.ContinuationsResponseHandler continuationsResponseHandler = receiver.processMessageAsync(request, response, continuation);
                continuation.setAttribute(RESPONSE_HANDLER_KEY, continuationsResponseHandler);
                // suspend indefinitely
                continuation.suspend();
            }
            else
            {
                // case where we are processing this request for the second time.
                if (continuation.isResumed())
                {
                    final JettyHttpMessageReceiver.ContinuationsResponseHandler responseHandler = (JettyHttpMessageReceiver.ContinuationsResponseHandler) continuation.getAttribute(RESPONSE_HANDLER_KEY);
                    responseHandler.complete();
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
