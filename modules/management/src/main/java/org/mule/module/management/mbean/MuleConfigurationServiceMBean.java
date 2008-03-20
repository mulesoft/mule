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
 * Mule server configuration.
 * 
 * TODO MULE-3110 I'm not sure this is relevant anymore since the MuleConfiguration should
 * be immutable after startup.
 */
public interface MuleConfigurationServiceMBean
{
    public boolean isSynchronous();

    //public void setSynchronous(boolean synchronous);

    public int getSynchronousEventTimeout();

    //public void setSynchronousEventTimeout(int synchronousEventTimeout);

    public boolean isRemoteSync();

    //public void setRemoteSync(boolean remoteSync);

    public String getWorkingDirectory();

    //public void setWorkingDirectory(String workingDirectory);

    public int getTransactionTimeout();

    //public void setTransactionTimeout(int transactionTimeout);

    public String getEncoding();

    //public void setEncoding(String encoding);
}
