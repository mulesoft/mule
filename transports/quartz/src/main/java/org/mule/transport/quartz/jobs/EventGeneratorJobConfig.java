/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.quartz.jobs;

import org.mule.transport.quartz.config.AbstractJobConfig;
import org.mule.transport.NullPayload;


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

    public Class getJobClass()
    {
        return EventGeneratorJob.class;
    }

}
