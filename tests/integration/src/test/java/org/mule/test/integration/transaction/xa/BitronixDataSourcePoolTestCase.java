/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transaction.xa;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mule.module.bti.BitronixConfigurationUtil.createUniqueIdForResource;

import org.mule.module.bti.jdbc.BitronixXaDataSourceWrapper;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.jdbc.JdbcConnector;

import bitronix.tm.resource.jdbc.PoolingDataSource;
import org.junit.Test;

public class BitronixDataSourcePoolTestCase extends FunctionalTestCase
{

    private final TransactionalTestSetUp testSetUp = JdbcDatabaseSetUp.createDatabaseOne();

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/transaction/xa/bitronix-data-source-pool-config.xml";
    }

    @Test
    public void createsDefaultPoolableConnectionForXADataSource()
    {
        String connectorName = "jdbcConnectorDefaultPool";
        PoolingDataSource dataSource = getDataSource(connectorName);
        String expectedName = createUniqueIdForResource(muleContext, connectorName);
        assertEquals(expectedName, dataSource.getUniqueName());
    }


    @Test
    public void parsesCustomDataSourcePoolCorrectly()
    {
        PoolingDataSource dataSource = getDataSource("jdbcConnectorCustomPool");
        String expectedName = createUniqueIdForResource(muleContext, "bitronixDataSource");
        assertEquals(5, dataSource.getMinPoolSize());
        assertEquals(15, dataSource.getMaxPoolSize());
        assertEquals(40, dataSource.getMaxIdleTime());
        assertEquals(expectedName, dataSource.getUniqueName());
        assertEquals(2, dataSource.getAcquireIncrement());
        assertEquals(6, dataSource.getPreparedStatementCacheSize());
        assertEquals(50, dataSource.getAcquisitionTimeout());
    }

    private PoolingDataSource getDataSource(String connectorName)
    {
        JdbcConnector connector = muleContext.getRegistry().get(connectorName);

        assertTrue(connector.getDataSource() instanceof BitronixXaDataSourceWrapper);

        BitronixXaDataSourceWrapper dataSourceWrapper = (BitronixXaDataSourceWrapper) connector.getDataSource();
        return dataSourceWrapper.getWrappedDataSource();
    }

    @Override
    protected void doSetUpBeforeMuleContextCreation() throws Exception
    {
        testSetUp.initialize();
    }

    @Override
    protected void doTearDownAfterMuleContextDispose() throws Exception
    {
        testSetUp.finalice();
    }
}
