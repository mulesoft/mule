/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.axis;

import org.mule.providers.soap.axis.mock.MockAxisServer;
import org.mule.providers.soap.axis.mock.MockProvider;
import org.mule.tck.FunctionalTestCase;

import junit.framework.Assert;

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
        
        Assert.assertNotNull(connector);
        Assert.assertEquals("test-axis-config.wsdd", connector.getServerConfig());
        Assert.assertEquals("test-axis-config.wsdd", connector.getClientConfig());
        Assert.assertFalse(connector.isTreatMapAsNamedParams());
        Assert.assertFalse(connector.isDoAutoTypes());
        Assert.assertEquals(2, connector.getBeanTypes().size());
        Assert.assertTrue(connector.getBeanTypes().contains("org.mule.tck.testmodels.fruit.Apple"));
        Assert.assertTrue(connector.getBeanTypes().contains("org.mule.tck.testmodels.fruit.Banana"));
        Assert.assertEquals(1, connector.getSupportedSchemes().size());
        Assert.assertEquals("http", connector.getSupportedSchemes().get(0));
    }

    public void testInjectedObjects()
    {
        AxisConnector connector = 
            (AxisConnector)managementContext.getRegistry().lookupConnector("axisConnector2");

        Assert.assertNotNull(connector);
        Assert.assertEquals(MockAxisServer.class, connector.getAxisServer().getClass());
        Assert.assertEquals(MockProvider.class, connector.getClientProvider().getClass());
    }
}


