/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.transport.jms.config;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import org.mule.runtime.transport.jms.CustomCachingConnectionFactory;
import org.mule.runtime.transport.jms.config.CachingConnectionFactoryFactoryBean;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import javax.jms.ConnectionFactory;

import org.junit.Test;

@SmallTest
public class CachingConnectionFactoryBeanTestCase extends AbstractMuleTestCase
{

    private final ConnectionFactory connectionFactoryDelegate = mock(ConnectionFactory.class);
    private final CachingConnectionFactoryFactoryBean factoryBean = new CachingConnectionFactoryFactoryBean();

    @Test
    public void buildsDefaultCachingConnectionFactory() throws Exception
    {
        factoryBean.setConnectionFactory(connectionFactoryDelegate);
        CustomCachingConnectionFactory connectionFactory = factoryBean.createInstance();
        assertThat(connectionFactory.getSessionCacheSize(), equalTo(CachingConnectionFactoryFactoryBean.DEFAULT_SESSION_CACHE_SIZE));
        assertThat(connectionFactory.isCacheProducers(), equalTo(false));
        assertThat(connectionFactory.isCacheConsumers(), equalTo(false));
        assertThat(connectionFactory.getUsername(), equalTo(null));
        assertThat(connectionFactory.getPassword(), equalTo(null));
    }

    @Test
    public void usesCustomCacheSize() throws Exception
    {
        int customSize = CachingConnectionFactoryFactoryBean.DEFAULT_SESSION_CACHE_SIZE + 5;
        factoryBean.setConnectionFactory(connectionFactoryDelegate);
        factoryBean.setSessionCacheSize(customSize);

        CustomCachingConnectionFactory connectionFactory = factoryBean.createInstance();

        assertThat(connectionFactory.getSessionCacheSize(), equalTo(customSize));
    }

    @Test
    public void usesCustomCacheProducers() throws Exception
    {
        final boolean customCacheProducers = true;
        factoryBean.setConnectionFactory(connectionFactoryDelegate);
        factoryBean.setCacheProducers(customCacheProducers);

        CustomCachingConnectionFactory connectionFactory = factoryBean.createInstance();

        assertThat(connectionFactory.isCacheProducers(), equalTo(customCacheProducers));
    }

    @Test
    public void usesCustomUser() throws Exception
    {
        final String username = "user";
        final String password = "password";

        factoryBean.setConnectionFactory(connectionFactoryDelegate);
        factoryBean.setUsername(username);
        factoryBean.setPassword(password);

        CustomCachingConnectionFactory connectionFactory = factoryBean.createInstance();

        assertThat(connectionFactory.getUsername(), equalTo(username));
        assertThat(connectionFactory.getPassword(), equalTo(password));
    }
}
