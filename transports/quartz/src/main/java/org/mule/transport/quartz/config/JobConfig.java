/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.quartz.config;

import java.io.Serializable;

import org.quartz.Job;

/**
 * The generic interface for Job Configuration endpoints that are described on Quartz
 * endpoints
 */
public interface JobConfig extends Serializable
{
    public Class<? extends Job> getJobClass();

    public String getGroupName();

    public String getJobGroupName();

    public void setGroupName(String groupName);

    public void setJobGroupName(String groupName);
}
