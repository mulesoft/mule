/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.endpoint.outbound;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transport.DispatchException;
import org.mule.context.notification.EndpointMessageNotification;
import org.mule.processor.AbstractInterceptingMessageProcessor;
import org.mule.transport.AbstractConnector;

/**
 * Publishes a {@link EndpointMessageNotification}'s when a message is sent or
 * dispatched.
 */
public class OutboundTryCatchMessageProcessor extends AbstractInterceptingMessageProcessor
{
    private OutboundEndpoint endpoint;
    private AbstractConnector connector;

    public OutboundTryCatchMessageProcessor(OutboundEndpoint endpoint)
    {
        this.endpoint = endpoint;
        this.connector = (AbstractConnector) endpoint.getConnector();
    }

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        boolean hasResponse = endpoint.getMessageExchangePattern().hasResponse();
        boolean isTransacted = endpoint.getTransactionConfig().isTransacted();
        boolean singleThread = hasResponse || isTransacted;

        try
        {
            return processNext(event);
        }
        catch (DispatchException e)
        {
            connector.handleException(e);
            if (singleThread)
            {
                throw e;
            }
            else
            {
                return null;
            }
        }
        catch (Exception e)
        {
            connector.handleException(e);
            if (singleThread)
            {
                throw new DispatchException(event.getMessage(), event.getEndpoint(), e);
            }
            else
            {
                return null;
            }
        }
    }
}
