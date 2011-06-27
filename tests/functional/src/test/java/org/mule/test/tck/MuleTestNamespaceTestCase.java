/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.tck;

import org.mule.api.transport.Connector;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.CounterCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.functional.ResponseWriterCallback;
import org.mule.tck.testmodels.mule.TestConnector;
import org.mule.transport.ConfigurableKeyedObjectPool;
import org.mule.transport.ConfigurableKeyedObjectPoolFactory;
import org.mule.transport.DefaultConfigurableKeyedObjectPool;
import org.mule.transport.DefaultConfigurableKeyedObjectPoolFactory;

import java.io.IOException;

public class MuleTestNamespaceTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "test-namespace-config.xml";
    }

    public void testComponent1Config() throws Exception
    {
        Object object = getComponent("testService1");
        assertNotNull(object);
        assertTrue(object instanceof FunctionalTestComponent);
        FunctionalTestComponent ftc = (FunctionalTestComponent) object;

        assertFalse(ftc.isEnableMessageHistory());
        assertFalse(ftc.isEnableNotifications());
        assertNull(ftc.getAppendString());
        assertEquals("Foo Bar Car Jar", ftc.getReturnData());
        assertNotNull(ftc.getEventCallback());
        assertTrue(ftc.getEventCallback() instanceof CounterCallback);
    }

    public void testComponent2Config() throws Exception
    {
        String testData = loadResourceAsString("test-data.txt");
        Object object = getComponent("testService2");
        assertNotNull(object);
        assertTrue(object instanceof FunctionalTestComponent);
        FunctionalTestComponent ftc = (FunctionalTestComponent) object;

        assertTrue(ftc.isThrowException());
        assertNotNull(ftc.getExceptionToThrow());
        assertTrue(ftc.getExceptionToThrow().isAssignableFrom(IOException.class));
        assertEquals("boom", ftc.getExceptionText());

        assertEquals(testData, ftc.getReturnData());

        assertTrue(ftc.isEnableMessageHistory());
        assertTrue(ftc.isEnableNotifications());
        assertNull(ftc.getAppendString());
        assertNotNull(ftc.getEventCallback());
        assertTrue(ftc.getEventCallback() instanceof ResponseWriterCallback);
    }

    public void testComponent3Config() throws Exception
    {
        Object object = getComponent("testService3");
        assertNotNull(object);
        assertTrue(object instanceof FunctionalTestComponent);
        FunctionalTestComponent ftc = (FunctionalTestComponent) object;

        assertFalse(ftc.isEnableMessageHistory());
        assertTrue(ftc.isEnableNotifications());
        assertEquals(" #[mule:context.serviceName]", ftc.getAppendString());
        assertNull(ftc.getReturnData());
        assertNull(ftc.getEventCallback());
    }

    public void testConnectorUsingDefaultDispatcherPoolFactory()
    {
        Connector connector = muleContext.getRegistry().lookupConnector("testConnectorWithDefaultFactory");

        assertTrue(connector instanceof TestConnector);
        TestConnector testConnector = (TestConnector) connector;
        assertEquals(DefaultConfigurableKeyedObjectPoolFactory.class, testConnector.getDispatcherPoolFactory().getClass());
        assertEquals(DefaultConfigurableKeyedObjectPool.class, testConnector.getDispatchers().getClass());
    }

    public void testConnectorUsingOverriddenDispatcherPoolFactory()
    {
        Connector connector = muleContext.getRegistry().lookupConnector("testConnectorWithOverriddenFactory");

        assertTrue(connector instanceof TestConnector);
        TestConnector testConnector = (TestConnector) connector;
        assertEquals(StubDispatcherPoolFactory.class, testConnector.getDispatcherPoolFactory().getClass());
        assertEquals(StubConfigurableKeyedObjectPool.class, testConnector.getDispatchers().getClass());
    }

    public static class StubConfigurableKeyedObjectPool extends DefaultConfigurableKeyedObjectPool
    {
        // no custom methods
    }

    public static class StubDispatcherPoolFactory implements ConfigurableKeyedObjectPoolFactory
    {
        @Override
        public ConfigurableKeyedObjectPool createObjectPool()
        {
            return new StubConfigurableKeyedObjectPool();
        }
    }
}
