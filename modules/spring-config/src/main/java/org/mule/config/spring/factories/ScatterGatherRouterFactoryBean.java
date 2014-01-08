/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.factories;

import org.mule.api.config.ThreadingProfile;
import org.mule.api.processor.MessageProcessor;
import org.mule.routing.EventMergeStrategy;
import org.mule.routing.ScatterGatherRouter;

import java.util.List;

import org.springframework.beans.factory.FactoryBean;

public class ScatterGatherRouterFactoryBean implements FactoryBean<ScatterGatherRouter>
{

    private long timeout = 0;
    private List<MessageProcessor> messageProcessors;
    private EventMergeStrategy eventMergeStrategy;
    private ThreadingProfile threadingProfile;

    @Override
    public ScatterGatherRouter getObject() throws Exception
    {
        ScatterGatherRouter sg = new ScatterGatherRouter();
        sg.setTimeout(timeout);

        for (MessageProcessor mp : this.messageProcessors)
        {
            sg.addRoute(mp);
        }

        if (this.eventMergeStrategy != null)
        {
            sg.setEventMergeStrategy(this.eventMergeStrategy);
        }

        if (this.threadingProfile != null)
        {
            sg.setThreadingProfile(this.threadingProfile);
        }

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

    public void setEventMergeStrategy(EventMergeStrategy eventMergeStrategy)
    {
        this.eventMergeStrategy = eventMergeStrategy;
    }

    public ThreadingProfile getThreadingProfile()
    {
        return this.threadingProfile;
    }

    public void setThreadingProfile(ThreadingProfile threadingProfile)
    {
        this.threadingProfile = threadingProfile;
    }
}
