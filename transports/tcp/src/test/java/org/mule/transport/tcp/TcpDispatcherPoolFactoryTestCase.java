/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.tcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mule.api.transport.Connector;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.ConfigurableKeyedObjectPool;
import org.mule.transport.ConfigurableKeyedObjectPoolFactory;
import org.mule.transport.DefaultConfigurableKeyedObjectPool;
import org.mule.transport.DefaultConfigurableKeyedObjectPoolFactory;

import org.junit.Test;

public class TcpDispatcherPoolFactoryTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "tcp-dispatcher-pool-factory-config.xml";
    }

    public static class StubConfigurableKeyedObjectPool extends DefaultConfigurableKeyedObjectPool
    {

    }

    public static class StubDispatcherPoolFactory implements ConfigurableKeyedObjectPoolFactory
    {
        @Override
        public ConfigurableKeyedObjectPool createObjectPool()
        {
            return new StubConfigurableKeyedObjectPool();
        }
    }

    @Test
    public void testConnectorUsingDefaultDispatcherPoolFactory()
    {
        Connector connector = muleContext.getRegistry().lookupConnector("tcpConnectorWithDefaultFactory");

        assertTrue(connector instanceof TcpConnector);
        TcpConnector tcpConnector = (TcpConnector) connector;
        assertEquals(DefaultConfigurableKeyedObjectPoolFactory.class, tcpConnector.getDispatcherPoolFactory().getClass());
        assertEquals(DefaultConfigurableKeyedObjectPool.class, tcpConnector.getDispatchers().getClass());
    }

    @Test
    public void testConnectorUsingOverriddenDispatcherPoolFactory()
    {
        Connector connector = muleContext.getRegistry().lookupConnector("tcpConnectorWithOverriddenFactory");

        assertTrue(connector instanceof TcpConnector);
        TcpConnector tcpConnector = (TcpConnector) connector;
        assertEquals(StubDispatcherPoolFactory.class, tcpConnector.getDispatcherPoolFactory().getClass());
        assertEquals(StubConfigurableKeyedObjectPool.class, tcpConnector.getDispatchers().getClass());
    }
}
