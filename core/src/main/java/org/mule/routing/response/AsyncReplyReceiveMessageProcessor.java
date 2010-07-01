/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.response;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.ResponseRouterCollection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class AsyncReplyReceiveMessageProcessor implements MessageProcessor
{
    protected Log logger = LogFactory.getLog(getClass());

    protected ResponseRouterCollection asyncReplyRouter;

    public AsyncReplyReceiveMessageProcessor(ResponseRouterCollection asyncReplyRouter)
    {
        this.asyncReplyRouter = asyncReplyRouter;
    }

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        if (event != null && !asyncReplyRouter.getRouters().isEmpty())
        {
            logger.debug("Waiting for response router message");
            event = asyncReplyRouter.getResponse(event);
        }
        return event;
    }
}
