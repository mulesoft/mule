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
import static org.mule.module.bti.BitronixConfigurationUtil.createUniqueIdForResource;
import org.mule.api.MuleContext;
import org.mule.api.config.MuleConfiguration;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.util.UUID;

import javax.sql.XADataSource;

import bitronix.tm.resource.jdbc.PoolingDataSource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@SmallTest
public class BitronixXaDataSourceBuilderTestCase extends AbstractMuleTestCase
{

    private static final String RESOURCE_NAME = "test";
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
        BitronixXaDataSourceBuilder builder = createBuilder(RESOURCE_NAME, mockDataSource);
        BitronixXaDataSourceWrapper wrapper = builder.build(mockMuleContext);
        PoolingDataSource poolingDataSource = wrapper.getWrappedDataSource();
        assertEquals(createUniqueIdForResource(mockMuleContext, RESOURCE_NAME), poolingDataSource.getUniqueName());
        assertEquals(BitronixXaDataSourceBuilder.DEFAULT_MAX_POOL_SIZE, poolingDataSource.getMaxPoolSize());
        assertEquals(BitronixXaDataSourceBuilder.DEFAULT_MIN_POOL_SIZE, poolingDataSource.getMinPoolSize());
        assertEquals(BitronixXaDataSourceBuilder.DEFAULT_MAX_IDLE, poolingDataSource.getMaxIdleTime());
        assertEquals(BitronixXaDataSourceBuilder.DEFAULT_ACQUISITION_TIMEOUT_SECONDS, poolingDataSource.getAcquisitionTimeout());
        assertEquals(BitronixXaDataSourceBuilder.DEFAULT_PREPARED_STATEMENT_CACHE_SIZE, poolingDataSource.getPreparedStatementCacheSize());
        assertEquals(BitronixXaDataSourceBuilder.DEFAULT_ACQUIRE_INCREMENT, poolingDataSource.getAcquireIncrement());
    }

    @Test
    public void createsDataSourcePoolWithSpecificValues()
    {
        final int customMinPoolSize = BitronixXaDataSourceBuilder.DEFAULT_MIN_POOL_SIZE + 1;
        final int customMaxPoolSize = BitronixXaDataSourceBuilder.DEFAULT_MAX_POOL_SIZE + 1;
        final int customMaxIdleTime = BitronixXaDataSourceBuilder.DEFAULT_MAX_IDLE + 1;
        final int customAcquisitionTimeout = BitronixXaDataSourceBuilder.DEFAULT_PREPARED_STATEMENT_CACHE_SIZE + 1;
        final int customPreparedStatementCacheSize = BitronixXaDataSourceBuilder.DEFAULT_PREPARED_STATEMENT_CACHE_SIZE + 1;
        final int customAcquireIncrement = BitronixXaDataSourceBuilder.DEFAULT_ACQUIRE_INCREMENT + 1;

        BitronixXaDataSourceBuilder builder = createBuilder("test", mockDataSource);
        builder.setMinPoolSize(customMinPoolSize);
        builder.setMaxPoolSize(customMaxPoolSize);
        builder.setMaxIdleTime(customMaxIdleTime);
        builder.setAcquisitionTimeoutSeconds(customAcquisitionTimeout);
        builder.setPreparedStatementCacheSize(customPreparedStatementCacheSize);
        builder.setAcquireIncrement(customAcquireIncrement);

        BitronixXaDataSourceWrapper wrapper = builder.build(mockMuleContext);
        PoolingDataSource poolingDataSource = wrapper.getWrappedDataSource();
        assertEquals(createUniqueIdForResource(mockMuleContext, RESOURCE_NAME), poolingDataSource.getUniqueName());
        assertEquals(customMinPoolSize, poolingDataSource.getMinPoolSize());
        assertEquals(customMaxPoolSize, poolingDataSource.getMaxPoolSize());
        assertEquals(customMaxIdleTime, poolingDataSource.getMaxIdleTime());
        assertEquals(customAcquisitionTimeout, poolingDataSource.getAcquisitionTimeout());
        assertEquals(customPreparedStatementCacheSize, poolingDataSource.getPreparedStatementCacheSize());
        assertEquals(customAcquireIncrement, poolingDataSource.getAcquireIncrement());
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
