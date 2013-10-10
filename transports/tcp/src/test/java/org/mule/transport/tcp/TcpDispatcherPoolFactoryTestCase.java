/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.tcp;

import org.mule.api.transport.Connector;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.ConfigurableKeyedObjectPool;
import org.mule.transport.ConfigurableKeyedObjectPoolFactory;
import org.mule.transport.DefaultConfigurableKeyedObjectPool;
import org.mule.transport.DefaultConfigurableKeyedObjectPoolFactory;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TcpDispatcherPoolFactoryTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "tcp-dispatcher-pool-factory-config.xml";
    }

    public static class StubConfigurableKeyedObjectPool extends DefaultConfigurableKeyedObjectPool
    {

    }

    public static class StubDispatcherPoolFactory implements ConfigurableKeyedObjectPoolFactory
    {

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
