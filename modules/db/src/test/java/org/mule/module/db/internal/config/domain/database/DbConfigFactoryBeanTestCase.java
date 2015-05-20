/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.config.domain.database;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import org.mule.module.db.internal.domain.database.DataSourceFactory;
import org.mule.module.db.internal.domain.type.DbType;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Collections;

import org.junit.Test;

@SmallTest
public class DbConfigFactoryBeanTestCase extends AbstractMuleTestCase
{

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
        dbConfigFactoryBean.createInstance();

        dbConfigFactoryBean.dispose();

        verify(dataSourceFactory).dispose();
    }
}