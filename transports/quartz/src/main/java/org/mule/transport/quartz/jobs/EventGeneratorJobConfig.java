/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.quartz.jobs;

import org.mule.transport.NullPayload;
import org.mule.transport.quartz.config.AbstractJobConfig;

import org.quartz.Job;
import org.quartz.StatefulJob;


/**
 * The configuration for the {@link EventGeneratorJob} job.
 */
public class EventGeneratorJobConfig extends AbstractJobConfig
{
    private Object payload = NullPayload.getInstance();

    public Object getPayload()
    {
        return payload;
    }

    public void setPayload(Object payload)
    {
        this.payload = payload;
    }

    @Override
    protected Class<? extends StatefulJob> getStatefulJobClass()
    {
        return StatefulEventGeneratorJob.class;
    }

    @Override
    protected Class<? extends Job> getStatelessJobClass()
    {
        return EventGeneratorJob.class;
    }
}
