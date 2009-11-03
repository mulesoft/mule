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

/**
 * The configuration for the {@link org.mule.transport.quartz.jobs.ScheduledDispatchJob} job.
 */
public class ScheduledDispatchJobConfig extends AbstractJobConfig
{
    /** The endpoint ref has t be a string and not a reference to the actual endpoint because
     * jobs can get stored in a database */
    private String endpointRef;

    public String getEndpointRef()
    {
        return endpointRef;
    }

    public void setEndpointRef(String endpointRef)
    {
        this.endpointRef = endpointRef;
    }

    protected Class getStatefulJobClass()
    {
        return StatefulScheduledDispatchJob.class;
    }

    protected Class getStatelessJobClass()
    {
        return ScheduledDispatchJob.class;
    }
}