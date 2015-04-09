/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.factories;

import org.mule.AbstractAnnotatedObject;
import org.mule.api.processor.MessageProcessor;
import org.mule.routing.AbstractSelectiveRouter;
import org.mule.routing.MessageProcessorFilterPair;

import java.util.Collection;

import org.springframework.beans.factory.FactoryBean;

public abstract class AbstractSelectiveRouterFactoryBean extends AbstractAnnotatedObject implements FactoryBean
{
    private MessageProcessor defaultProcessor;
    private Collection<MessageProcessorFilterPair> conditionalMessageProcessors;

    public AbstractSelectiveRouterFactoryBean()
    {
        super();
    }

    public void setDefaultRoute(MessageProcessorFilterPair conditionalProcessor)
    {
        defaultProcessor = conditionalProcessor.getMessageProcessor();
    }

    public void setRoutes(Collection<MessageProcessorFilterPair> conditionalMessageProcessors)
    {
        this.conditionalMessageProcessors = conditionalMessageProcessors;
    }

    public Object getObject() throws Exception
    {
        final AbstractSelectiveRouter router = newAbstractSelectiveRouter();
        router.setAnnotations(getAnnotations());
        router.setDefaultRoute(defaultProcessor);

        for (final MessageProcessorFilterPair mpfp : conditionalMessageProcessors)
        {
            router.addRoute(mpfp.getMessageProcessor(), mpfp.getFilter());
        }

        return router;
    }

    protected abstract AbstractSelectiveRouter newAbstractSelectiveRouter();

    public boolean isSingleton()
    {
        return true;
    }
}
