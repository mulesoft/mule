/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
 *
 * Deprecated from 3.6.0.  This functionality is specific to Services.
 */
@Deprecated
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
