/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.management.mbean;

import org.mule.api.MuleException;

import java.util.Date;

/**
 * <code>MuleServiceMBean</code> is a JMX service interface for the MuleContext.
 * 
 */
public interface MuleServiceMBean
{
    boolean isInitialised();

    boolean isStopped();

    Date getStartTime();

    String getVersion();

    String getVendor();

    void start() throws MuleException;

    void stop() throws MuleException;

    void dispose() throws MuleException;

    long getFreeMemory();

    long getMaxMemory();

    long getTotalMemory();

    String getServerId();

    String getHostname();

    String getHostIp();

    String getOsVersion();

    String getJdkVersion();

    String getCopyright();

    String getLicense();

    String getBuildDate();

    String getBuildNumber();

    String getInstanceId();

    /**
     * Contains value of option -builder
     *
     * @return builder class name
     */
    String getConfigBuilderClassName();
}
