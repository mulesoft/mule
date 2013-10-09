/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.transport;

import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class LifecycleTrackerConnectorFunctionalTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/transport/connector-lifecycle-config.xml";
    }

    /**
     * ASSERT:
     * - Mule stop/start lifecycle methods invoked
     * - Mule initialize/dipose lifecycle methods NOT invoked
     * - Spring lifecycle methods invoked
     * - Service and muleContext injected (Component implements ServiceAware/MuleContextAware)
     * NOTE: muleContext is injected twice, once by registry and once by lifecycleAdaptor
     * @throws Exception
     */
    @Test
    public void testConnectorLifecycle() throws Exception
    {
        testComponentLifecycle(
            "test1",
            "[setProperty, initialise, connect, start, stop, disconnect, dispose]");
    }

    private void testComponentLifecycle(final String connectorName, final String expectedLifeCycle)
        throws Exception
    {

        final ConnectorLifecycleTracker tracker = getConnector(connectorName);

        muleContext.dispose();

        assertEquals(connectorName, expectedLifeCycle, tracker.getTracker().toString());
    }

    private ConnectorLifecycleTracker getConnector(final String connectorName) throws Exception
    {
        ConnectorLifecycleTracker t = (ConnectorLifecycleTracker)muleContext.getRegistry().lookupConnector(connectorName);
        assertNotNull(t);

        return t;
    }
}
