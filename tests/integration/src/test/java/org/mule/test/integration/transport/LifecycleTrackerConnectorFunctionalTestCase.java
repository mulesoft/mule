/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.functional.junit4.FunctionalTestCase;

import org.junit.Test;

public class LifecycleTrackerConnectorFunctionalTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
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
