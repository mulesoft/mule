/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.management.mbeans;

import org.mule.config.MuleConfiguration;

/**
 * <code>MuleConfigurationService</code> exposes the MuleConfiguration settings as
 * a management service
 * 
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

    public void setSynchronous(boolean synchronous)
    {
        muleConfiguration.setDefaultSynchronousEndpoints(synchronous);
    }

    public int getSynchronousEventTimeout()
    {
        return muleConfiguration.getDefaultSynchronousEventTimeout();
    }

    public void setSynchronousEventTimeout(int synchronousEventTimeout)
    {
        muleConfiguration.setDefaultSynchronousEventTimeout(synchronousEventTimeout);
    }

    public boolean isRemoteSync()
    {
        return muleConfiguration.isDefaultRemoteSync();
    }

    public void setRemoteSync(boolean remoteSync)
    {
        muleConfiguration.setDefaultRemoteSync(remoteSync);
    }


    public String getWorkingDirectory()
    {
        return muleConfiguration.getWorkingDirectory();
    }

    public void setWorkingDirectory(String workingDirectory)
    {
        muleConfiguration.setWorkingDirectory(workingDirectory);
    }

    public String[] getConfigResources()
    {
        return muleConfiguration.getConfigResources();
    }


    public int getTransactionTimeout()
    {
        return muleConfiguration.getDefaultTransactionTimeout();
    }

    public void setTransactionTimeout(int transactionTimeout)
    {
        muleConfiguration.setDefaultTransactionTimeout(transactionTimeout);
    }

    public boolean isClientMode()
    {
        return muleConfiguration.isClientMode();
    }


    public String getEncoding()
    {
        return muleConfiguration.getDefaultEncoding();
    }

    public void setEncoding(String encoding)
    {
        muleConfiguration.setDefaultEncoding(encoding);
    }

    public String getOSEncoding()
    {
        return muleConfiguration.getDefaultOSEncoding();
    }

    public void setOSEncoding(String encoding)
    {
        muleConfiguration.setDefaultOSEncoding(encoding);
    }
}
