/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.management.mbean;

/**
 * <code>MuleConfigurationServiceMBean</code> is a JMx service interface for the
 * Mule server configuration. This is read-only after start-up
 * 
 */
public interface MuleConfigurationServiceMBean
{
    String DEFAULT_JMX_NAME = "name=Configuration";

    int getSynchronousEventTimeout();

    String getWorkingDirectory();

    int getTransactionTimeout();
    
    int getShutdownTimeout();
    
    String getEncoding();

    boolean isContainerMode();

    boolean isFullStackTraces();

    void setFullStackTraces(boolean sanitize);

    String getStackTraceFilter();

    /**
     * Comma-separated list of packages and/or classes to remove.
     */
    void setStackTraceFilter(String filterAsString);
}
