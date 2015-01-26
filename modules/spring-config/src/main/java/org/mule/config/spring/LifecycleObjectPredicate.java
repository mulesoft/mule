/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import org.mule.api.MuleContext;
import org.mule.api.component.Component;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.routing.OutboundRouterCollection;
import org.mule.api.source.MessageSource;
import org.mule.api.transformer.Transformer;
import org.mule.processor.AbstractMessageProcessorOwner;
import org.mule.routing.requestreply.AbstractAsyncRequestReplyRequester;

import org.apache.commons.collections.Predicate;

final class LifecycleObjectPredicate implements Predicate
{

    private final Class<?>[] ignoredTypes = new Class[] {
            Component.class,
            MessageSource.class,
            AbstractMessageProcessorOwner.class,
            MessagingExceptionHandler.class,
            Transformer.class,
            AbstractAsyncRequestReplyRequester.class,
            OutboundRouterCollection.class,
            MuleContext.class
    };

    @Override
    public boolean evaluate(Object o)
    {
        Class<?> clazz = o.getClass();
        for (Class<?> ignoredType : ignoredTypes)
        {
            if (ignoredType.isAssignableFrom(clazz))
            {
                return false;
            }
        }

        return true;
    }
}
