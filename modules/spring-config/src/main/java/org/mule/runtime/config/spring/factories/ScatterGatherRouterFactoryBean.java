/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.config.spring.factories;

import org.mule.runtime.core.AbstractAnnotatedObject;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ThreadingProfile;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.routing.AggregationStrategy;
import org.mule.runtime.core.routing.ScatterGatherRouter;

import java.util.List;

import org.springframework.beans.factory.FactoryBean;

public class ScatterGatherRouterFactoryBean extends AbstractAnnotatedObject implements FactoryBean<ScatterGatherRouter>, MuleContextAware, FlowConstructAware
{

    private long timeout = 0;
    private List<MessageProcessor> messageProcessors;
    private AggregationStrategy aggregationStrategy;
    private ThreadingProfile threadingProfile;
    private MuleContext muleContext;
    private FlowConstruct flowConstruct;

    @Override
    public ScatterGatherRouter getObject() throws Exception
    {
        ScatterGatherRouter sg = new ScatterGatherRouter();
        sg.setTimeout(timeout);
        sg.setMuleContext(muleContext);
        sg.setFlowConstruct(flowConstruct);

        for (MessageProcessor mp : this.messageProcessors)
        {
            sg.addRoute(mp);
        }

        if (this.aggregationStrategy != null)
        {
            sg.setAggregationStrategy(this.aggregationStrategy);
        }

        if (this.threadingProfile != null)
        {
            sg.setThreadingProfile(this.threadingProfile);
        }

        sg.setAnnotations(getAnnotations());
        return sg;
    }

    @Override
    public Class<?> getObjectType()
    {
        return ScatterGatherRouter.class;
    }

    @Override
    public boolean isSingleton()
    {
        return false;
    }

    public void setMessageProcessors(List<MessageProcessor> messageProcessors)
    {
        this.messageProcessors = messageProcessors;
    }

    public void setTimeout(long timeout)
    {
        this.timeout = timeout;
    }

    public void setAggregationStrategy(AggregationStrategy aggregationStrategy)
    {
        this.aggregationStrategy = aggregationStrategy;
    }

    public ThreadingProfile getThreadingProfile()
    {
        return this.threadingProfile;
    }

    public void setThreadingProfile(ThreadingProfile threadingProfile)
    {
        this.threadingProfile = threadingProfile;
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    @Override
    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;
    }
}
