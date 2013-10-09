/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.routing.requestreply;

import org.mule.api.MuleEvent;
import org.mule.construct.Flow;

public class AsyncReplyToPropertyRequestReplyReplier extends AbstractReplyToPropertyRequestReplyReplier
{

    @Override
    protected boolean shouldProcessEvent(MuleEvent event)
    {
        return !event.getExchangePattern().hasResponse() && (event.getFlowConstruct() instanceof Flow);
    }

}
