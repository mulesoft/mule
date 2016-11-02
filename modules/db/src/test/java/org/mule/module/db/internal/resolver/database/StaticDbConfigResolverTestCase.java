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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import org.mule.api.MuleEvent;
import org.mule.api.lifecycle.Disposable;
import org.mule.common.Result;
import org.mule.common.TestResult;
import org.mule.common.metadata.MetaData;
import org.mule.common.metadata.MetaDataKey;
import org.mule.module.db.internal.domain.database.DbConfig;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.List;

import javax.sql.DataSource;

import org.junit.Test;

public class StaticDbConfigResolverTestCase extends AbstractMuleTestCase
{

    private final DbConfig dbConfig = mock(DbConfig.class);
    private final StaticDbConfigResolver dbConfigResolver = new StaticDbConfigResolver(dbConfig);

    @Test
    public void resolvesDbConfig() throws Exception
    {
        MuleEvent muleEvent = mock(MuleEvent.class);

        DbConfig resolvedDbConfig = dbConfigResolver.resolve(muleEvent);

        assertThat(resolvedDbConfig, is(dbConfig));
    }

    @Test
    public void testsConnection() throws Exception
    {
        TestResult expectedTestResult = mock(TestResult.class);
        when(dbConfig.test()).thenReturn(expectedTestResult);

        TestResult testResult = dbConfigResolver.test();

        assertThat(testResult, is(expectedTestResult));
    }

    @Test
    public void returnsMetaDataKeys() throws Exception
    {
        final Result<List<MetaDataKey>> expectedMetaDataResult = mock(Result.class);
        when(dbConfig.getMetaDataKeys()).thenReturn(expectedMetaDataResult);

        final Result<List<MetaDataKey>> metaDataResult = dbConfigResolver.getMetaDataKeys();

        assertThat(metaDataResult, is(expectedMetaDataResult));
    }

    @Test
    public void returnsMetaData() throws Exception
    {
        final Result<MetaData> expectedMetaData = mock(Result.class);
        final MetaDataKey metaDataKey = mock(MetaDataKey.class);
        when(dbConfig.getMetaData(metaDataKey)).thenReturn(expectedMetaData);

        final Result<MetaData> metaData = dbConfigResolver.getMetaData(metaDataKey);

        assertThat(metaData, is(expectedMetaData));
    }

    @Test
    public void disposesDisposableDbConfig() throws Exception
    {
        DisposableDataSource dataSource = mock(DisposableDataSource.class);
        when(dbConfig.getDataSource()).thenReturn(dataSource);

        dbConfigResolver.dispose();

        verify(dataSource, times(1)).dispose();
    }

    private interface DisposableDataSource extends DataSource, Disposable {

    }
}