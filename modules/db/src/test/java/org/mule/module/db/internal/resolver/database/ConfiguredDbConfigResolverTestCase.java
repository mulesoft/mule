/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.resolver.database;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.api.MuleEvent;
import org.mule.common.Result;
import org.mule.common.TestResult;
import org.mule.common.metadata.MetaData;
import org.mule.common.metadata.MetaDataKey;
import org.mule.module.db.internal.domain.database.DbConfig;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.List;

import org.junit.Test;

@SmallTest
public class ConfiguredDbConfigResolverTestCase extends AbstractMuleTestCase
{

    private DbConfigResolver dbConfigResolver = mock(DbConfigResolver.class);
    private ConfiguredDbConfigResolver configuredDbConfigResolver = new ConfiguredDbConfigResolver(dbConfigResolver);
    private DbConfig dbConfig = mock(DbConfig.class);

    @Test
    public void resolvesDbConfig() throws Exception
    {
        MuleEvent muleEvent = mock(MuleEvent.class);
        when(dbConfigResolver.resolve(muleEvent)).thenReturn(dbConfig);

        DbConfig resolvedDbConfig = configuredDbConfigResolver.resolve(muleEvent);

        assertThat(resolvedDbConfig, is(dbConfig));
    }

    @Test
    public void testsConnection() throws Exception
    {
        TestResult expectedTestResult = mock(TestResult.class);
        when(dbConfig.test()).thenReturn(expectedTestResult);
        when(dbConfigResolver.resolve(null)).thenReturn(dbConfig);

        TestResult testResult = configuredDbConfigResolver.test();

        assertThat(testResult, is(expectedTestResult));
    }

    @Test
    public void returnsMetaDataKeys() throws Exception
    {
        final Result<List<MetaDataKey>> expectedMetaDataResult = mock(Result.class);
        when(dbConfig.getMetaDataKeys()).thenReturn(expectedMetaDataResult);
        when(dbConfigResolver.resolve(null)).thenReturn(dbConfig);

        final Result<List<MetaDataKey>> metaDataResult = configuredDbConfigResolver.getMetaDataKeys();

        assertThat(metaDataResult, is(expectedMetaDataResult));
    }

    @Test
    public void returnsMetaData() throws Exception
    {
        final Result<MetaData> expectedMetaData = mock(Result.class);
        final MetaDataKey metaDataKey = mock(MetaDataKey.class);
        when(dbConfig.getMetaData(metaDataKey)).thenReturn(expectedMetaData);
        when(dbConfigResolver.resolve(null)).thenReturn(dbConfig);

        final Result<MetaData> metaData = configuredDbConfigResolver.getMetaData(metaDataKey);

        assertThat(metaData, is(expectedMetaData));
    }
}