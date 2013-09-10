/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.processor;

import org.mule.api.MessagingException;
import org.mule.api.processor.InterceptingMessageProcessor;
import org.mule.api.source.MessageSource;
import org.mule.routing.requestreply.AbstractAsyncRequestReplyRequester;
import org.mule.service.ServiceAsyncReplyCompositeMessageSource;
@Deprecated
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
