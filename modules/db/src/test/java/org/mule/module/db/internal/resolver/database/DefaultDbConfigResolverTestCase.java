/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.resolver.database;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.api.MuleEvent;
import org.mule.api.registry.MuleRegistry;
import org.mule.common.Result;
import org.mule.common.TestResult;
import org.mule.common.metadata.MetaData;
import org.mule.common.metadata.MetaDataKey;
import org.mule.module.db.internal.domain.database.DbConfig;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;

@SmallTest
public class DefaultDbConfigResolverTestCase extends AbstractMuleTestCase
{

    private final MuleEvent muleEvent = mock(MuleEvent.class);

    @Test
    public void resolvesDefaultDbConfig() throws Exception
    {
        DbConfigResolver dbConfigResolver = mock(DbConfigResolver.class);
        DbConfig dbConfig = mock(DbConfig.class);
        when(dbConfigResolver.resolve(muleEvent)).thenReturn(dbConfig);

        MuleRegistry registry = createMockRegistry(dbConfigResolver);
        DefaultDbConfigResolver defaultDbConfigResolver = new DefaultDbConfigResolver(registry);

        DbConfig resolvedDbConfig = defaultDbConfigResolver.resolve(muleEvent);

        assertThat(dbConfig, sameInstance(resolvedDbConfig));
    }

    @Test(expected = UnresolvableDbConfigException.class)
    public void throwsErrorWhenNoDbConfigAvailable() throws Exception
    {
        MuleRegistry registry = mock(MuleRegistry.class);
        Collection<DbConfig> foundDbConfigs = new ArrayList<DbConfig>();

        when(registry.lookupObjects(DbConfig.class)).thenReturn(foundDbConfigs);

        DefaultDbConfigResolver dbConfigResolver = new DefaultDbConfigResolver(registry);

        dbConfigResolver.resolve(muleEvent);
    }

    @Test
    public void throwsErrorWhenMultipleDbConfigAvailable() throws Exception
    {
        DbConfig dbConfig1 = mock(DbConfig.class);
        when(dbConfig1.getName()).thenReturn("dbConfig1");
        DbConfig dbConfig2 = mock(DbConfig.class);
        when(dbConfig2.getName()).thenReturn("dbConfig2");

        DbConfigResolver dbConfigResolver1 = mock(DbConfigResolver.class);
        when(dbConfigResolver1.resolve(null)).thenReturn(dbConfig1);

        DbConfigResolver dbConfigResolver2 = mock(DbConfigResolver.class);
        when(dbConfigResolver2.resolve(null)).thenReturn(dbConfig2);

        MuleRegistry registry = mock(MuleRegistry.class);
        Collection<DbConfigResolver> foundDbConfigResolvers = new ArrayList<>();
        foundDbConfigResolvers.add(dbConfigResolver1);
        foundDbConfigResolvers.add(dbConfigResolver2);

        when(registry.lookupObjects(DbConfigResolver.class)).thenReturn(foundDbConfigResolvers);

        DefaultDbConfigResolver dbConfigResolver = new DefaultDbConfigResolver(registry);

        try
        {
            dbConfigResolver.resolve(muleEvent);
            fail("Was supposed to fail when there are multiple dbConfigs available");
        }
        catch (UnresolvableDbConfigException e)
        {
            assertThat(e.getMessage(), containsString("dbConfig1"));
            assertThat(e.getMessage(), containsString("dbConfig2"));
        }
    }

    @Test
    public void testsConnection() throws Exception
    {
        DbConfig dbConfig = mock(DbConfig.class);
        TestResult expectedTestResult = mock(TestResult.class);
        when(dbConfig.test()).thenReturn(expectedTestResult);

        DbConfigResolver dbConfigResolver = mock(DbConfigResolver.class);
        when(dbConfigResolver.resolve(null)).thenReturn(dbConfig);

        MuleRegistry registry = createMockRegistry(dbConfigResolver);
        DefaultDbConfigResolver defaultDbConfigResolver = new DefaultDbConfigResolver(registry);

        final TestResult testResult = defaultDbConfigResolver.test();

        assertThat(testResult, is(expectedTestResult));
    }


    @Test
    public void returnsMetaDataKeys() throws Exception
    {
        DbConfig dbConfig = mock(DbConfig.class);
        final Result<List<MetaDataKey>> expectedMetaDataResult = mock(Result.class);
        when(dbConfig.getMetaDataKeys()).thenReturn(expectedMetaDataResult);

        DbConfigResolver dbConfigResolver = mock(DbConfigResolver.class);
        when(dbConfigResolver.resolve(null)).thenReturn(dbConfig);

        MuleRegistry registry = createMockRegistry(dbConfigResolver);
        DefaultDbConfigResolver defaultDbConfigResolver = new DefaultDbConfigResolver(registry);

        final Result<List<MetaDataKey>> metaDataResult = defaultDbConfigResolver.getMetaDataKeys();

        assertThat(metaDataResult, is(expectedMetaDataResult));
    }

    @Test
    public void returnsMetaData() throws Exception
    {
        DbConfig dbConfig = mock(DbConfig.class);
        final Result<MetaData> expectedMetaData = mock(Result.class);
        final MetaDataKey metaDataKey = mock(MetaDataKey.class);
        when(dbConfig.getMetaData(metaDataKey)).thenReturn(expectedMetaData);

        DbConfigResolver dbConfigResolver = mock(DbConfigResolver.class);
        when(dbConfigResolver.resolve(null)).thenReturn(dbConfig);

        MuleRegistry registry = createMockRegistry(dbConfigResolver);
        DefaultDbConfigResolver defaultDbConfigResolver = new DefaultDbConfigResolver(registry);

        final Result<MetaData> metaData = defaultDbConfigResolver.getMetaData(metaDataKey);

        assertThat(metaData, is(expectedMetaData));
    }

    private MuleRegistry createMockRegistry(DbConfigResolver dbConfigResolver)
    {
        Collection<DbConfigResolver> foundDbConfigResolvers = new ArrayList<>();
        foundDbConfigResolvers.add(dbConfigResolver);

        MuleRegistry registry = mock(MuleRegistry.class);
        when(registry.lookupObjects(DbConfigResolver.class)).thenReturn(foundDbConfigResolvers);

        return registry;
    }
}
