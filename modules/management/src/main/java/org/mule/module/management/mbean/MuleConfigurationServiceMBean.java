/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
