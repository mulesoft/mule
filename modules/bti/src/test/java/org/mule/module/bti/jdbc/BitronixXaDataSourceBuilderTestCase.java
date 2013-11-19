/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.bti.jdbc;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.api.MuleContext;
import org.mule.api.config.MuleConfiguration;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.util.UUID;

import javax.sql.XADataSource;

import bitronix.tm.resource.jdbc.PoolingDataSource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;


public class BitronixXaDataSourceBuilderTestCase extends AbstractMuleTestCase
{

    private final MuleContext mockMuleContext = mock(MuleContext.class, Answers.RETURNS_DEEP_STUBS.get());
    private final MuleConfiguration mockMuleConfiguration = mock(MuleConfiguration.class);
    private final XADataSource mockDataSource = mock(XADataSource.class, Answers.RETURNS_DEEP_STUBS.get());
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
    public void createsDataSourcePoolWithDefaultValues()
    {
        BitronixXaDataSourceBuilder builder = createBuilder("test", mockDataSource);
        BitronixXaDataSourceWrapper wrapper = builder.build(mockMuleContext);
        PoolingDataSource poolingDataSource = wrapper.getWrappedDataSource();
        assertEquals(muleConfigurationId + "-test", poolingDataSource.getUniqueName());
    }

    @Test
    public void createsDataSourcePoolWithSpecificValues()
    {
        BitronixXaDataSourceBuilder builder = createBuilder("test", mockDataSource);
        builder.setMinPoolSize(8);
        builder.setMaxPoolSize(10);
        builder.setMaxIdleTime(30);

        BitronixXaDataSourceWrapper wrapper = builder.build(mockMuleContext);
        PoolingDataSource poolingDataSource = wrapper.getWrappedDataSource();
        assertEquals(muleConfigurationId + "-test", poolingDataSource.getUniqueName());
        assertEquals(8, poolingDataSource.getMinPoolSize());
        assertEquals(10, poolingDataSource.getMaxPoolSize());
        assertEquals(30, poolingDataSource.getMaxIdleTime());
    }

    @Test(expected = IllegalStateException.class)
    public void failsToCreateDataSourcePoolWithoutName()
    {
        BitronixXaDataSourceBuilder builder = createBuilder(null, mockDataSource);
        builder.build(mockMuleContext);
    }

    @Test(expected = IllegalStateException.class)
    public void failsToCreateDataSourcePoolWithoutDataSource()
    {
        BitronixXaDataSourceBuilder builder = createBuilder("test", null);
        builder.build(mockMuleContext);
    }

    private BitronixXaDataSourceBuilder createBuilder(String name, XADataSource dataSource)
    {
        BitronixXaDataSourceBuilder builder = new BitronixXaDataSourceBuilder();
        builder.setName(name);
        builder.setDataSource(dataSource);
        return builder;
    }

}
