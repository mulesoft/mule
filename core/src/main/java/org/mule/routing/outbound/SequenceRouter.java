/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.routing.outbound;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;

/**
 * Defines a {@link AbstractSequenceRouter} that stops the routing of a given
 * message when a synchronous endpoint has returned a null or an exception
 * message.
 * <p/>
 * Asynchronous endpoints are managed as in the {@link AbstractSequenceRouter}.
 */
public class SequenceRouter extends AbstractSequenceRouter
{

    /**
     * Determines if the routing should continue after receiving a given
     * response from an synchronous endpoint.
     *
     * @param event the last received response event
     * @return true if the message is not null and is not an exception message.
     *         False otherwise.
     */
    @Override
    protected boolean continueRoutingMessageAfter(MuleEvent event)
    {
        boolean result = true;

        MuleMessage muleMessage = event.getMessage();

        if (muleMessage == null || muleMessage.getExceptionPayload() != null)
        {
            logger.warn("Sequence router will stop routing current message");
            result = false;
        }

        return result;
    }
}
