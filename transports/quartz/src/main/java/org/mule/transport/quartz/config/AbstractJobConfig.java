/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.quartz.config;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.transport.quartz.QuartzConnector;

/**
 * Base implementation of {@link org.mule.transport.quartz.config.JobConfig}.
 */
public abstract class AbstractJobConfig implements JobConfig, MuleContextAware
{
    private String groupName = QuartzConnector.DEFAULT_GROUP_NAME;

    private String jobGroupName = QuartzConnector.DEFAULT_GROUP_NAME;

    private MuleContext muleContext;

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    public MuleContext getMuleContext()
    {
        return muleContext;
    }

    public String getGroupName()
    {
        return groupName;
    }

    public void setGroupName(String groupName)
    {
        this.groupName = groupName;
    }

    public String getJobGroupName()
    {
        return jobGroupName;
    }

    public void setJobGroupName(String jobGroupName)
    {
        this.jobGroupName = jobGroupName;
    }
}
