/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.factories;

import java.util.Collection;

import org.mule.api.processor.MessageProcessor;
import org.mule.routing.AbstractSelectiveRouter;
import org.mule.routing.ConditionalMessageProcessor;
import org.springframework.beans.factory.FactoryBean;

public abstract class AbstractSelectiveRouterFactoryBean implements FactoryBean
{
    private MessageProcessor defaultProcessor;
    private Collection<ConditionalMessageProcessor> conditionalMessageProcessors;

    public AbstractSelectiveRouterFactoryBean()
    {
        super();
    }

    public void setDefaultRoute(ConditionalMessageProcessor conditionalProcessor)
    {
        defaultProcessor = conditionalProcessor.getMessageProcessor();
    }

    public void setRoutes(Collection<ConditionalMessageProcessor> conditionalMessageProcessors)
    {
        this.conditionalMessageProcessors = conditionalMessageProcessors;
    }

    public Object getObject() throws Exception
    {
        final AbstractSelectiveRouter router = newAbstractSelectiveRouter();
        router.setDefaultRoute(defaultProcessor);

        for (final ConditionalMessageProcessor cmp : conditionalMessageProcessors)
        {
            router.addRoute(cmp);
        }

        return router;
    }

    protected abstract AbstractSelectiveRouter newAbstractSelectiveRouter();

    public boolean isSingleton()
    {
        return true;
    }

}
