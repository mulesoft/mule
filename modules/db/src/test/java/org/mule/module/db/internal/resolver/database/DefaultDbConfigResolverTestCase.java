/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.resolver.database;

import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.api.MuleEvent;
import org.mule.api.registry.MuleRegistry;
import org.mule.module.db.internal.domain.database.DbConfig;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;

@SmallTest
public class DefaultDbConfigResolverTestCase extends AbstractMuleTestCase
{

    private final MuleEvent muleEvent = mock(MuleEvent.class);

    @Test
    public void resolvesDefaultDbConfig() throws Exception
    {
        DbConfig dbConfig = mock(DbConfig.class);

        MuleRegistry registry = mock(MuleRegistry.class);
        Collection<DbConfig> foundDbConfigs = new ArrayList<DbConfig>();
        foundDbConfigs.add(dbConfig);

        when(registry.lookupObjects(DbConfig.class)).thenReturn(foundDbConfigs);
        DefaultDbConfigResolver dbConfigResolver = new DefaultDbConfigResolver(registry);

        DbConfig resolvedDbConfig = dbConfigResolver.resolve(muleEvent);

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

        MuleRegistry registry = mock(MuleRegistry.class);
        Collection<DbConfig> foundDbConfigs = new ArrayList<DbConfig>();
        foundDbConfigs.add(dbConfig1);
        foundDbConfigs.add(dbConfig2);

        when(registry.lookupObjects(DbConfig.class)).thenReturn(foundDbConfigs);

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
}
