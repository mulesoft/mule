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

import org.mule.api.config.MuleConfiguration;

/**
 * <code>MuleConfigurationService</code> exposes the MuleConfiguration settings as
 * a management service
 * 
 * TODO MULE-3110 I'm not sure this is relevant anymore since the MuleConfiguration should
 * be immutable after startup.
 */
public class MuleConfigurationService implements MuleConfigurationServiceMBean
{
    private  MuleConfiguration muleConfiguration;

    public MuleConfigurationService(MuleConfiguration muleConfiguration)
    {
        this.muleConfiguration = muleConfiguration;
    }

    public boolean isSynchronous()
    {
        return muleConfiguration.isDefaultSynchronousEndpoints();
    }

//    public void setSynchronous(boolean synchronous)
//    {
//        muleConfiguration.setDefaultSynchronousEndpoints(synchronous);
//    }

    public int getSynchronousEventTimeout()
    {
        return muleConfiguration.getDefaultResponseTimeout();
    }

    public String getWorkingDirectory()
    {
        return muleConfiguration.getWorkingDirectory();
    }

    public int getTransactionTimeout()
    {
        return muleConfiguration.getDefaultTransactionTimeout();
    }

    public int getShutdownTimeout()
    {
        return muleConfiguration.getShutdownTimeout();
    }

    public boolean isClientMode()
    {
        return muleConfiguration.isClientMode();
    }


    public String getEncoding()
    {
        return muleConfiguration.getDefaultEncoding();
    }

}
