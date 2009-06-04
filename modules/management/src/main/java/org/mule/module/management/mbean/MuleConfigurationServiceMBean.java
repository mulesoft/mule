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


/**
 * <code>MuleConfigurationServiceMBean</code> is a JMx service interface for the
 * Mule server configuration. This is read-only after start-up
 * 
 */
public interface MuleConfigurationServiceMBean
{
    public boolean isSynchronous();

    public int getSynchronousEventTimeout();

    public String getWorkingDirectory();

    public int getTransactionTimeout();
    
    public int getShutdownTimeout();
    
    public String getEncoding();
}
