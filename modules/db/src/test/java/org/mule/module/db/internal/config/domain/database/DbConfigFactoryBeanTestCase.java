/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.config.domain.database;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.api.config.MuleProperties.OBJECT_DEFAULT_RETRY_POLICY_TEMPLATE;
import org.mule.api.MuleContext;
import org.mule.api.registry.MuleRegistry;
import org.mule.module.db.internal.domain.database.DataSourceFactory;
import org.mule.module.db.internal.domain.type.DbType;
import org.mule.retry.policies.NoRetryPolicyTemplate;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class DbConfigFactoryBeanTestCase extends AbstractMuleTestCase
{

    final MuleContext muleContext = mock(MuleContext.class);
    final MuleRegistry muleRegistry = mock(MuleRegistry.class);
    private final NoRetryPolicyTemplate noRetryPolicyTemplate = mock(NoRetryPolicyTemplate.class);

    @Before
    public void setUp() throws Exception
    {
        when(muleContext.getRegistry()).thenReturn(muleRegistry);
        when(muleRegistry.lookupObject(OBJECT_DEFAULT_RETRY_POLICY_TEMPLATE)).thenReturn(noRetryPolicyTemplate);
    }

    @Test
    public void disposesDataSourceFactory() throws Exception
    {
        final DataSourceFactory dataSourceFactory = mock(DataSourceFactory.class);
        DbConfigResolverFactoryBean dbConfigFactoryBean = new DbConfigResolverFactoryBean()
        {
            @Override
            protected DataSourceFactory createDataSourceFactory()
            {
                return dataSourceFactory;
            }
        };

        dbConfigFactoryBean.setCustomDataTypes(Collections.<DbType>emptyList());
        dbConfigFactoryBean.setMuleContext(muleContext);
        dbConfigFactoryBean.createInstance();
        dbConfigFactoryBean.dispose();
        verify(dataSourceFactory).dispose();
    }

}