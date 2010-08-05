/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.requestreply;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.source.MessageSource;

public class SimpleAsyncRequestReplyRequester extends AbstractAsyncRequestReplyRequester
{

    @Override
    protected MuleEvent processNext(MuleEvent event) throws MuleException
    {
        setAsyncReplyProperties(event);
        return super.processNext(event);
    }

    protected void setAsyncReplyProperties(MuleEvent event) throws MuleException
    {
        event.getMessage().setReplyTo(getReplyTo());
        event.getMessage().setOutboundProperty(MuleProperties.MULE_REPLY_TO_REQUESTOR_PROPERTY,
            flowConstruct.getName());
        String correlation = flowConstruct.getMessageInfoMapping().getCorrelationId(event.getMessage());
        event.getMessage().setCorrelationId(correlation);
    }

    private String getReplyTo()
    {
        return ((InboundEndpoint) replyMessageSource).getEndpointURI().getAddress();
    }

    @Override
    protected void verifyReplyMessageSource(MessageSource messageSource)
    {
        if (!(messageSource instanceof InboundEndpoint))
        {
            throw new IllegalArgumentException(
                "Only an InboundEndpoint reply MessageSource is supported with SimpleAsyncRequestReplyRequester");
        }
    }
}
