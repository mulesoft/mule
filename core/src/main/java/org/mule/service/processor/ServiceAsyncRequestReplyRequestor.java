/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.service.processor;

import org.mule.api.MessagingException;
import org.mule.api.processor.InterceptingMessageProcessor;
import org.mule.api.source.MessageSource;
import org.mule.routing.requestreply.AbstractAsyncRequestReplyRequester;
import org.mule.service.ServiceAsyncReplyCompositeMessageSource;

public class ServiceAsyncRequestReplyRequestor extends AbstractAsyncRequestReplyRequester implements InterceptingMessageProcessor
{

    protected void postLatchAwait(String asyncReplyCorrelationId) throws MessagingException
    {
        if (replyMessageSource instanceof ServiceAsyncReplyCompositeMessageSource)
        {
            ((ServiceAsyncReplyCompositeMessageSource) replyMessageSource).expireAggregation(asyncReplyCorrelationId);
        }
    }

    @Override
    protected void verifyReplyMessageSource(MessageSource messageSource)
    {
        if (!(messageSource instanceof ServiceAsyncReplyCompositeMessageSource))
        {
            throw new IllegalArgumentException(
                "ServiceAsyncReplyCompositeMessageSource async reply MessageSource must be used with ServiceAsyncRequestReplyRequestor");
        }
    }
}
