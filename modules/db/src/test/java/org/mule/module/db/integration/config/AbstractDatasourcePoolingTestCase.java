/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.config;

import org.mule.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.module.db.integration.TestDbConfig;
import org.mule.module.db.integration.model.AbstractTestDatabase;

import java.util.List;

import org.junit.Before;
import org.junit.runners.Parameterized;

public abstract class AbstractDatasourcePoolingTestCase extends AbstractDbIntegrationTestCase
{

    protected static int counter;

    public AbstractDatasourcePoolingTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
    {
        super(dataSourceConfigResource, testDatabase);
    }

    @Parameterized.Parameters
    public static List<Object[]> parameters()
    {
        return TestDbConfig.getDerbyResource();
    }

    @Before
    public void setUp() throws Exception
    {
        counter = 0;
    }

    public static class Counter
    {

        public static synchronized Object process(Object payload)
        {
            counter++;
            return payload;
        }
    }
}
