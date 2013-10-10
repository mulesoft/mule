/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
    // CRITICAL: do not modify the order of key/value pairs here, it MUST
    // match the one returned by ObjectName.getCanonicalKeyPropertyListString()
    String DEFAULT_JMX_NAME = "name=MuleContext";

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
