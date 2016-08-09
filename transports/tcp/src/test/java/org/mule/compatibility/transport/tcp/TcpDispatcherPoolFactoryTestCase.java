/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.tcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mule.compatibility.core.api.transport.Connector;
import org.mule.compatibility.core.transport.ConfigurableKeyedObjectPool;
import org.mule.compatibility.core.transport.ConfigurableKeyedObjectPoolFactory;
import org.mule.compatibility.core.transport.DefaultConfigurableKeyedObjectPool;
import org.mule.compatibility.core.transport.DefaultConfigurableKeyedObjectPoolFactory;
import org.mule.compatibility.transport.tcp.TcpConnector;
import org.mule.functional.junit4.FunctionalTestCase;

import org.junit.Test;

public class TcpDispatcherPoolFactoryTestCase extends FunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "tcp-dispatcher-pool-factory-config.xml";
  }

  public static class StubConfigurableKeyedObjectPool extends DefaultConfigurableKeyedObjectPool {

  }

  public static class StubDispatcherPoolFactory implements ConfigurableKeyedObjectPoolFactory {

    @Override
    public ConfigurableKeyedObjectPool createObjectPool() {
      return new StubConfigurableKeyedObjectPool();
    }
  }

  @Test
  public void testConnectorUsingDefaultDispatcherPoolFactory() {
    Connector connector = muleContext.getRegistry().lookupObject("tcpConnectorWithDefaultFactory");

    assertTrue(connector instanceof TcpConnector);
    TcpConnector tcpConnector = (TcpConnector) connector;
    assertEquals(DefaultConfigurableKeyedObjectPoolFactory.class, tcpConnector.getDispatcherPoolFactory().getClass());
    assertEquals(DefaultConfigurableKeyedObjectPool.class, tcpConnector.getDispatchers().getClass());
  }

  @Test
  public void testConnectorUsingOverriddenDispatcherPoolFactory() {
    Connector connector = muleContext.getRegistry().lookupObject("tcpConnectorWithOverriddenFactory");

    assertTrue(connector instanceof TcpConnector);
    TcpConnector tcpConnector = (TcpConnector) connector;
    assertEquals(StubDispatcherPoolFactory.class, tcpConnector.getDispatcherPoolFactory().getClass());
    assertEquals(StubConfigurableKeyedObjectPool.class, tcpConnector.getDispatchers().getClass());
  }
}
