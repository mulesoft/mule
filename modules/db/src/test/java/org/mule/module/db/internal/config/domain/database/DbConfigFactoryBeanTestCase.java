/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.config.domain.database;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import org.mule.module.db.internal.domain.database.GenericDbConfig;
import org.mule.module.db.internal.domain.type.DbType;
import org.mule.module.db.internal.domain.type.DbTypeManager;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Collections;

import javax.sql.DataSource;

import org.junit.Test;

@SmallTest
public class DbConfigFactoryBeanTestCase extends AbstractMuleTestCase
{

    @Test
    public void disposesDbConfig() throws Exception
    {
        final GenericDbConfig dbConfig = spy(new GenericDbConfig(null, "test", null));

        DbConfigFactoryBean dbConfigFactoryBean = new DbConfigFactoryBean()
        {
            @Override
            protected GenericDbConfig doCreateDbConfig(DataSource datasource, DbTypeManager dbTypeManager)
            {
                return dbConfig;
            }
        };
        dbConfigFactoryBean.setCustomDataTypes(Collections.<DbType>emptyList());
        dbConfigFactoryBean.createInstance();

        dbConfigFactoryBean.dispose();

        verify(dbConfig).dispose();
    }
}