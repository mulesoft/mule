/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.axis;

import org.mule.providers.soap.axis.mock.MockAxisServer;
import org.mule.providers.soap.axis.mock.MockProvider;
import org.mule.tck.FunctionalTestCase;

public class AxisNamespaceHandlerTestCase extends FunctionalTestCase
{   
    protected String getConfigResources()
    {
        return "axis-namespace-config.xml";
    }

    public void testConfig()
    {
        AxisConnector connector = 
            (AxisConnector)managementContext.getRegistry().lookupConnector("axisConnector");
        
        assertNotNull(connector);
        assertEquals("test-axis-config.wsdd", connector.getServerConfig());
        assertEquals("test-axis-config.wsdd", connector.getClientConfig());
        assertFalse(connector.isTreatMapAsNamedParams());
        assertFalse(connector.isDoAutoTypes());
        assertEquals(2, connector.getBeanTypes().size());
        assertTrue(connector.getBeanTypes().contains("org.mule.tck.testmodels.fruit.Apple"));
        assertTrue(connector.getBeanTypes().contains("org.mule.tck.testmodels.fruit.Banana"));
        assertEquals(1, connector.getSupportedSchemes().size());
        assertEquals("http", connector.getSupportedSchemes().get(0));
    }

    public void testInjectedObjects()
    {
        AxisConnector connector = 
            (AxisConnector)managementContext.getRegistry().lookupConnector("axisConnector2");

        assertNotNull(connector);
        assertEquals(MockAxisServer.class, connector.getAxis().getClass());
        assertEquals(MockProvider.class, connector.getClientProvider().getClass());
    }
}


