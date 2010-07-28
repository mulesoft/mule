/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.service.processor;

import org.mule.routing.asyncreply.DefaultAsyncReplyMessageProcessor;
import org.mule.service.ServiceAsyncReplyCompositeMessageSource;

public class ServiceAsyncReplyMessageProcessor extends DefaultAsyncReplyMessageProcessor
{

    protected void postLatchAwait(String asyncReplyCorrelationId)
    {
        if (replyMessageSource instanceof ServiceAsyncReplyCompositeMessageSource)
        {
            ((ServiceAsyncReplyCompositeMessageSource) replyMessageSource).expireAggregation(asyncReplyCorrelationId);
        }
    }

}
