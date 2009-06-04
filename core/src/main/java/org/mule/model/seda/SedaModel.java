/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.model.seda;

import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.PoolingProfile;
import org.mule.config.QueueProfile;
import org.mule.model.AbstractModel;

/**
 * A mule service service model that uses Seda principals to achieve high
 * throughput by Quing events for compoonents and processing them concurrently.
 */
public class SedaModel extends AbstractModel
{
    /**
     * The time out used for taking from the Seda Queue.
     */
    private Integer queueTimeout;

    /**
     * the pooling configuration used when initialising the service described by
     * this descriptor.
     */
    protected PoolingProfile poolingProfile;

    /**
     * The queuing profile for events received for this service
     */
    protected QueueProfile queueProfile;

    /**
     * Returns the model type name. This is a friendly identifier that is used to
     * look up the SPI class for the model
     * 
     * @return the model type
     */
    public String getType()
    {
        return "seda";
    }

    public void initialise() throws InitialisationException
    {
        if (queueTimeout == null)
        {
            queueTimeout = muleContext.getConfiguration().getDefaultQueueTimeout();
        }
        if (queueProfile == null)
        {
            queueProfile = new QueueProfile();
        }
        if (poolingProfile == null)
        {
            poolingProfile = new PoolingProfile();
        }
        super.initialise();
    }

    public int getQueueTimeout()
    {
        return queueTimeout;
    }

    public void setQueueTimeout(int queueTimeout)
    {
        this.queueTimeout = queueTimeout;
    }

    public PoolingProfile getPoolingProfile()
    {
        return poolingProfile;
    }

    public void setPoolingProfile(PoolingProfile poolingProfile)
    {
        this.poolingProfile = poolingProfile;
    }

    public QueueProfile getQueueProfile()
    {
        return queueProfile;
    }

    public void setQueueProfile(QueueProfile queueProfile)
    {
        this.queueProfile = queueProfile;
    }
}
