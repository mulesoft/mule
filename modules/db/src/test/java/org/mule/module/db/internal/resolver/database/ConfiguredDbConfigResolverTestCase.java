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
import org.mule.module.db.internal.domain.database.DbConfig;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class ConfiguredDbConfigResolverTestCase extends AbstractMuleTestCase
{

    @Test
    public void resolvesDbConfig() throws Exception
    {
        MuleEvent muleEvent = mock(MuleEvent.class);
        DbConfig dbConfig = mock(DbConfig.class);
        DbConfigResolver dbConfigResolver = mock(DbConfigResolver.class);
        when(dbConfigResolver.resolve(muleEvent)).thenReturn(dbConfig);
        ConfiguredDbConfigResolver configuredDbConfigResolver = new ConfiguredDbConfigResolver(dbConfigResolver);

        DbConfig resolvedDbConfig = configuredDbConfigResolver.resolve(muleEvent);

        assertThat(resolvedDbConfig, is(dbConfig));
    }
}