/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transaction.xa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.module.bti.jms.BitronixConnectionFactoryWrapper;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.jms.JmsConnector;

import bitronix.tm.resource.jms.PoolingConnectionFactory;
import org.junit.ClassRule;
import org.junit.Test;


public class BitronixConnectionFactoryPoolTestCase extends FunctionalTestCase
{

    @ClassRule
    public static DynamicPort port = new DynamicPort("port");

    private final TransactionalTestSetUp testSetUp = new JmsBrokerSetUp(port.getNumber());

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/transaction/xa/bitronix-connection-factory-pool-config.xml";
    }

    @Test
    public void createsDefaultConnectionFactoryPoolForXADataSource()
    {
        PoolingConnectionFactory poolingConnectionFactory = getConnectionFactory("jmsConnectorDefaultPool");
        String expectedDefaultName = muleContext.getConfiguration().getId() + "-jmsConnectorDefaultPool";

        assertEquals(expectedDefaultName, poolingConnectionFactory.getUniqueName());
    }

    @Test
    public void parsesCustomConnectionFactoryPoolCorrectly()
    {
        PoolingConnectionFactory poolingConnectionFactory = getConnectionFactory("jmsConnectorCustomPool");
        String expectedName = muleContext.getConfiguration().getId() + "-bitronixConnectionFactory";

        assertEquals(5, poolingConnectionFactory.getMinPoolSize());
        assertEquals(15, poolingConnectionFactory.getMaxPoolSize());
        assertEquals(40, poolingConnectionFactory.getMaxIdleTime());
        assertEquals(expectedName, poolingConnectionFactory.getUniqueName());
    }

    private PoolingConnectionFactory getConnectionFactory(String connectorName)
    {
        JmsConnector connector = muleContext.getRegistry().get(connectorName);

        assertNotNull(connector);
        assertTrue(connector.getConnectionFactory() instanceof BitronixConnectionFactoryWrapper);

        BitronixConnectionFactoryWrapper connectionFactoryWrapper = (BitronixConnectionFactoryWrapper) connector.getConnectionFactory();
        return connectionFactoryWrapper.getWrappedConnectionFactory();
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
