/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.transport.jms.config;

import org.mule.runtime.transport.jms.CustomCachingConnectionFactory;

import javax.jms.ConnectionFactory;

import org.springframework.beans.factory.config.AbstractFactoryBean;

/**
 * DEPRECATED: This element is deprecated from Mule 3.6.
 * This can still but used in 3.6, but it not necessary given that from Mule 3.6 JMS connections cache Sessions/Producers
 * by default when a CachingConnectionFactory has not been configured explicitly.
 */
@Deprecated
public class CachingConnectionFactoryFactoryBean extends AbstractFactoryBean<CustomCachingConnectionFactory>
{

    public static final int DEFAULT_SESSION_CACHE_SIZE = 1;

    private String name;
    private boolean cacheProducers;
    private ConnectionFactory connectionFactory;
    private int sessionCacheSize = DEFAULT_SESSION_CACHE_SIZE;
    private String username;
    private String password;

    @Override
    public Class<?> getObjectType()
    {
        return CustomCachingConnectionFactory.class;
    }

    @Override
    protected CustomCachingConnectionFactory createInstance() throws Exception
    {
        CustomCachingConnectionFactory cachingConnectionFactory = new CustomCachingConnectionFactory(connectionFactory,
                                                                                                     username,
                                                                                                     password);
        cachingConnectionFactory.setCacheProducers(cacheProducers);
        cachingConnectionFactory.setSessionCacheSize(sessionCacheSize);
        cachingConnectionFactory.setCacheConsumers(false);

        return cachingConnectionFactory;
    }

    @Override
    protected void destroyInstance(CustomCachingConnectionFactory instance) throws Exception
    {
        instance.destroy();
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public boolean isCacheProducers()
    {
        return cacheProducers;
    }

    public void setCacheProducers(boolean cacheProducers)
    {
        this.cacheProducers = cacheProducers;
    }

    public ConnectionFactory getConnectionFactory()
    {
        return connectionFactory;
    }

    public void setConnectionFactory(ConnectionFactory connectionFactory)
    {
        this.connectionFactory = connectionFactory;
    }

    public int getSessionCacheSize()
    {
        return sessionCacheSize;
    }

    public void setSessionCacheSize(int sessionCacheSize)
    {
        this.sessionCacheSize = sessionCacheSize;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }
}
