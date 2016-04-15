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
import java.util.concurrent.CountDownLatch;

import org.junit.Before;
import org.junit.runners.Parameterized;

public abstract class AbstractDatasourcePoolingTestCase extends AbstractDbIntegrationTestCase
{

    protected static final int TOTAL_CONCURRENT_REQUESTS = 2;
    protected static CountDownLatch connectionLatch;

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
        connectionLatch = new CountDownLatch(TOTAL_CONCURRENT_REQUESTS);
    }

    public static class JoinRequests
    {

        public static Object process(Object payload)
        {
            connectionLatch.countDown();

            try
            {
                connectionLatch.await();
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }

            return payload;
        }
    }
}
