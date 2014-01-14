/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.bti.jms;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.module.bti.BitronixConfigurationUtil.createUniqueIdForResource;

import org.mule.api.MuleContext;
import org.mule.api.config.MuleConfiguration;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.util.UUID;

import javax.jms.ConnectionFactory;
import javax.jms.XAConnectionFactory;

import bitronix.tm.resource.jms.PoolingConnectionFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@SmallTest
public class BitronixConnectionFactoryPoolBuilderTestCase extends AbstractMuleTestCase
{

    public static final String RESOURCE_NAME = "test";
    private final MuleContext mockMuleContext = mock(MuleContext.class, Answers.RETURNS_DEEP_STUBS.get());
    private final MuleConfiguration mockMuleConfiguration = mock(MuleConfiguration.class);
    private final XAConnectionFactory mockConnectionFactory = mock(TestConnectionFactory.class);
    private final String muleConfigurationId = UUID.getUUID();

    @Before
    public void setUp()
    {
        when(mockMuleContext.getConfiguration()).thenReturn(mockMuleConfiguration);
        when(mockMuleConfiguration.getId()).thenAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                return muleConfigurationId;
            }
        });
    }

    @Test
    public void createsConnectionFactoryPoolWithDefaultValues()
    {
        BitronixConnectionFactoryPoolBuilder builder = createBuilder(RESOURCE_NAME, mockConnectionFactory);
        BitronixConnectionFactoryWrapper wrapper = builder.build(mockMuleContext);
        PoolingConnectionFactory poolingConnectionFactory = wrapper.getWrappedConnectionFactory();
        assertEquals(createUniqueIdForResource(mockMuleContext, RESOURCE_NAME), poolingConnectionFactory.getUniqueName());
    }

    @Test
    public void createsConnectionFactoryPoolWithSpecificValues()
    {
        BitronixConnectionFactoryPoolBuilder builder = createBuilder("test", mockConnectionFactory);
        builder.setMinPoolSize(8);
        builder.setMaxPoolSize(10);
        builder.setMaxIdleTime(30);

        BitronixConnectionFactoryWrapper wrapper = builder.build(mockMuleContext);
        PoolingConnectionFactory poolingConnectionFactory = wrapper.getWrappedConnectionFactory();
        assertEquals(createUniqueIdForResource(mockMuleContext, RESOURCE_NAME), poolingConnectionFactory.getUniqueName());
        assertEquals(8, poolingConnectionFactory.getMinPoolSize());
        assertEquals(10, poolingConnectionFactory.getMaxPoolSize());
        assertEquals(30, poolingConnectionFactory.getMaxIdleTime());
    }

    @Test(expected = IllegalStateException.class)
    public void failsToCreateConnectionFactoryWithoutName()
    {
        BitronixConnectionFactoryPoolBuilder builder = createBuilder(null, mockConnectionFactory);
        builder.build(mockMuleContext);
    }

    @Test(expected = IllegalStateException.class)
    public void failsToCreateConnectionFactoryWithoutConnectionFactory()
    {
        BitronixConnectionFactoryPoolBuilder builder = createBuilder("test", null);
        builder.build(mockMuleContext);
    }

    private BitronixConnectionFactoryPoolBuilder createBuilder(String name, XAConnectionFactory connectionFactory)
    {
        BitronixConnectionFactoryPoolBuilder builder = new BitronixConnectionFactoryPoolBuilder();
        builder.setName(name);
        builder.setConnectionFactory(connectionFactory);
        return builder;
    }

    private interface TestConnectionFactory extends ConnectionFactory, XAConnectionFactory
    {
        // Custom connection factory interface to create a mock that implements two interfaces.
    }
}
