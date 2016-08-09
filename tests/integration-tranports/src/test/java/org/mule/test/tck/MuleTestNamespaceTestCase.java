/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.tck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mule.compatibility.core.api.transport.Connector;
import org.mule.compatibility.core.transport.ConfigurableKeyedObjectPool;
import org.mule.compatibility.core.transport.ConfigurableKeyedObjectPoolFactory;
import org.mule.compatibility.core.transport.DefaultConfigurableKeyedObjectPool;
import org.mule.compatibility.core.transport.DefaultConfigurableKeyedObjectPoolFactory;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.tck.testmodels.mule.TestConnector;

import org.junit.Test;

public class MuleTestNamespaceTestCase extends FunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "test-namespace-config-flow.xml";
  }

  @Test
  public void testConnectorUsingDefaultDispatcherPoolFactory() {
    Connector connector = muleContext.getRegistry().lookupObject("testConnectorWithDefaultFactory");

    assertTrue(connector instanceof TestConnector);
    TestConnector testConnector = (TestConnector) connector;
    assertEquals(DefaultConfigurableKeyedObjectPoolFactory.class, testConnector.getDispatcherPoolFactory().getClass());
    assertEquals(DefaultConfigurableKeyedObjectPool.class, testConnector.getDispatchers().getClass());
  }

  @Test
  public void testConnectorUsingOverriddenDispatcherPoolFactory() {
    Connector connector = muleContext.getRegistry().lookupObject("testConnectorWithOverriddenFactory");

    assertTrue(connector instanceof TestConnector);
    TestConnector testConnector = (TestConnector) connector;
    assertEquals(StubDispatcherPoolFactory.class, testConnector.getDispatcherPoolFactory().getClass());
    assertEquals(StubConfigurableKeyedObjectPool.class, testConnector.getDispatchers().getClass());
  }

  public static class StubConfigurableKeyedObjectPool extends DefaultConfigurableKeyedObjectPool {
    // no custom methods
  }

  public static class StubDispatcherPoolFactory implements ConfigurableKeyedObjectPoolFactory {

    @Override
    public ConfigurableKeyedObjectPool createObjectPool() {
      return new StubConfigurableKeyedObjectPool();
    }
  }
}
