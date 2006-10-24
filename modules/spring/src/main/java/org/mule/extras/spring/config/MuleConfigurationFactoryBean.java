/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.spring.config;

import org.mule.MuleManager;
import org.mule.config.MuleConfiguration;
import org.mule.config.PoolingProfile;
import org.mule.config.QueueProfile;
import org.mule.config.ThreadingProfile;
import org.springframework.beans.factory.FactoryBean;

/**
 * <code>MuleConfigurationFactoryBean</code> is used to configure the MuleManager
 * object. This is not necessary if you use the AutowireUMOManagerFactoryBean as it
 * handles creating the MuleConfiguration correctly for you.
 * 
 * @deprecated use AutowireUMOManagerFactoryBean instead
 * @see AutowireUMOManagerFactoryBean
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class MuleConfigurationFactoryBean implements FactoryBean
{
    private final MuleConfiguration muleConfiguration = MuleManager.getConfiguration();

    public Object getObject() throws Exception
    {
        return muleConfiguration;
    }

    public Class getObjectType()
    {
        return MuleConfiguration.class;
    }

    public boolean isSingleton()
    {
        return true;
    }

    public boolean isSynchronous()
    {
        return muleConfiguration.isSynchronous();
    }

    public void setSynchronous(boolean synchronous)
    {
        muleConfiguration.setSynchronous(synchronous);
    }

    public String getModel()
    {
        return muleConfiguration.getModel();
    }

    public void setModel(String model)
    {
        muleConfiguration.setModel(model);
    }

    public ThreadingProfile getMessageDispatcherThreadingProfile()
    {
        return muleConfiguration.getMessageDispatcherThreadingProfile();
    }

    public void setMessageDispatcherThreadingProfile(ThreadingProfile messageDispatcherThreadingProfile)
    {
        muleConfiguration.setMessageDispatcherThreadingProfile(messageDispatcherThreadingProfile);
    }

    public ThreadingProfile getMessageReceiverThreadingProfile()
    {
        return muleConfiguration.getMessageReceiverThreadingProfile();
    }

    public void setMessageReceiverThreadingProfile(ThreadingProfile messageReceiverThreadingProfile)
    {
        muleConfiguration.setMessageReceiverThreadingProfile(messageReceiverThreadingProfile);
    }

    public ThreadingProfile getComponentThreadingProfile()
    {
        return muleConfiguration.getComponentThreadingProfile();
    }

    public void setComponentThreadingProfile(ThreadingProfile componentPoolThreadingProfile)
    {
        muleConfiguration.setComponentThreadingProfile(componentPoolThreadingProfile);
    }

    public ThreadingProfile getDefaultThreadingProfile()
    {
        return muleConfiguration.getDefaultThreadingProfile();
    }

    public void setDefaultThreadingProfile(ThreadingProfile defaultThreadingProfile)
    {
        muleConfiguration.setDefaultThreadingProfile(defaultThreadingProfile);
    }

    public PoolingProfile getPoolingProfile()
    {
        return muleConfiguration.getPoolingProfile();
    }

    public void setPoolingProfile(PoolingProfile poolingProfile)
    {
        muleConfiguration.setPoolingProfile(poolingProfile);
    }

    public int getSynchronousEventTimeout()
    {
        return muleConfiguration.getSynchronousEventTimeout();
    }

    public void setSynchronousEventTimeout(int synchronousEventTimeout)
    {
        muleConfiguration.setSynchronousEventTimeout(synchronousEventTimeout);
    }

    public boolean isSynchronousReceive()
    {
        return muleConfiguration.isRemoteSync();
    }

    public void setSynchronousReceive(boolean synchronousReceive)
    {
        muleConfiguration.setRemoteSync(synchronousReceive);
    }

    public QueueProfile getQueueProfile()
    {
        return muleConfiguration.getQueueProfile();
    }

    public void setQueueProfile(QueueProfile queueProfile)
    {
        muleConfiguration.setQueueProfile(queueProfile);
    }

    public boolean isRecoverableMode()
    {
        return muleConfiguration.isRecoverableMode();
    }

    public void setRecoverableMode(boolean recoverableMode)
    {
        muleConfiguration.setRecoverableMode(recoverableMode);
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

    public void setConfigResources(String[] configResources)
    {
        muleConfiguration.setConfigResources(configResources);
    }

    public String getServerUrl()
    {
        return muleConfiguration.getServerUrl();
    }

    public void setServerUrl(String serverUrl)
    {
        muleConfiguration.setServerUrl(serverUrl);
    }
}
