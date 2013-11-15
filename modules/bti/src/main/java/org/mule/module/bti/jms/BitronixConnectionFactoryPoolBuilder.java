/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.bti.jms;


import org.mule.api.MuleContext;
import org.mule.util.Preconditions;

import javax.jms.ConnectionFactory;
import javax.jms.XAConnectionFactory;

import bitronix.tm.resource.jms.PoolingConnectionFactory;

public class BitronixConnectionFactoryPoolBuilder
{

    private int minPoolSize = 2;
    private int maxPoolSize = 32;
    private int maxIdleTime = 60;
    private String name;
    private XAConnectionFactory connectionFactory;

    public BitronixConnectionFactoryWrapper build(MuleContext muleContext)
    {
        Preconditions.checkState(name != null, "name is required");
        Preconditions.checkState(connectionFactory != null, "connectionFactory is required");
        Preconditions.checkState(minPoolSize > 0, "minPoolSize must be greater than 0");
        Preconditions.checkState(maxPoolSize > 0, "maxPoolSize must be greater than 0");
        Preconditions.checkState(maxIdleTime > 0, "maxIdleTime must be greater than 0");

        PoolingConnectionFactory poolingConnectionFactory;

        synchronized (BitronixJmsXaConnectionFactoryProvider.class)
        {
            //TODO change once BTM-131 get's fixed
            BitronixJmsXaConnectionFactoryProvider.xaConnectionFactoryProvided = (ConnectionFactory) connectionFactory;
            poolingConnectionFactory = new PoolingConnectionFactory();
            poolingConnectionFactory.setClassName(BitronixJmsXaConnectionFactoryProvider.class.getCanonicalName());
            poolingConnectionFactory.setAutomaticEnlistingEnabled(false);
            poolingConnectionFactory.setMaxPoolSize(maxPoolSize);
            poolingConnectionFactory.setMinPoolSize(minPoolSize);
            poolingConnectionFactory.setMaxIdleTime(maxIdleTime);
            poolingConnectionFactory.setCacheProducersConsumers(false);
            poolingConnectionFactory.setAllowLocalTransactions(true);
            poolingConnectionFactory.setUniqueName(muleContext.getConfiguration().getId() + "-" + name);
            poolingConnectionFactory.init();
        }
        return new BitronixConnectionFactoryWrapper(poolingConnectionFactory, muleContext);
    }

    public int getMinPoolSize()
    {
        return minPoolSize;
    }

    public void setMinPoolSize(int minPoolSize)
    {
        this.minPoolSize = minPoolSize;
    }

    public int getMaxPoolSize()
    {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize)
    {
        this.maxPoolSize = maxPoolSize;
    }

    public int getMaxIdleTime()
    {
        return maxIdleTime;
    }

    public void setMaxIdleTime(int maxIdleTime)
    {
        this.maxIdleTime = maxIdleTime;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public XAConnectionFactory getConnectionFactory()
    {
        return connectionFactory;
    }

    public void setConnectionFactory(XAConnectionFactory connectionFactory)
    {
        this.connectionFactory = connectionFactory;
    }
}
