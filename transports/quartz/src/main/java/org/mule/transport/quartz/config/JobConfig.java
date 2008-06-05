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

import java.io.Serializable;

/**
 * The generic interface for Job Configuration endpoints that are described on Quartz endpoints
 */
public interface JobConfig extends Serializable
{
    public Class getJobClass();

    public String getGroupName();

    public String getJobGroupName();

    public void setGroupName(String groupName);

    public void setJobGroupName(String groupName);
}
