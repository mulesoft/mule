/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.management.mbeans;

import org.mule.MuleManager;
import org.mule.config.MuleConfiguration;
import org.mule.config.PoolingProfile;
import org.mule.config.QueueProfile;
import org.mule.config.ThreadingProfile;

/**
 * <code>MuleConfigurationService</code> exposes the MuleConfiguration
 * settings as a management service
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class MuleConfigurationService implements MuleConfigurationServiceMBean
{
    private final MuleConfiguration config = MuleManager.getConfiguration();

    public boolean isSynchronous()
    {
        return config.isSynchronous();
    }

    public void setSynchronous(boolean synchronous)
    {
        config.setSynchronous(synchronous);
    }

    public ThreadingProfile getMessageDispatcherThreadingProfile()
    {
        return config.getMessageDispatcherThreadingProfile();
    }

    public ThreadingProfile getMessageReceiverThreadingProfile()
    {
        return config.getMessageReceiverThreadingProfile();
    }

    public ThreadingProfile getComponentThreadingProfile()
    {
        return config.getComponentThreadingProfile();
    }

    public ThreadingProfile getDefaultThreadingProfile()
    {
        return config.getDefaultThreadingProfile();
    }

    public PoolingProfile getPoolingProfile()
    {
        return config.getPoolingProfile();
    }

    public int getSynchronousEventTimeout()
    {
        return config.getSynchronousEventTimeout();
    }

    public void setSynchronousEventTimeout(int synchronousEventTimeout)
    {
        config.setSynchronousEventTimeout(synchronousEventTimeout);
    }

    public boolean isSynchronousReceive()
    {
        return config.isSynchronousReceive();
    }

    public QueueProfile getQueueProfile()
    {
        return config.getQueueProfile();
    }

    public boolean isRecoverableMode()
    {
        return config.isRecoverableMode();
    }

    public String getWorkingDirectoy()
    {
        return config.getWorkingDirectoy();
    }

    public String[] getConfigResources()
    {
        return config.getConfigResources();
    }

    public String getServerUrl()
    {
        return config.getServerUrl();
    }
}
