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
import org.mule.umo.endpoint.UMOImmutableEndpoint;

import java.util.Map;
import java.util.List;

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

    public void testEndpointProperties() throws Exception
    {
        UMOImmutableEndpoint endpoint =
                managementContext.getRegistry().lookupEndpointBuilder("endpoint").buildOutboundEndpoint();
        Map props = endpoint.getProperties();
        assertEquals("[methodNamespace][method]", assertKey(props, "soapAction", String.class));
        assertEquals("echo,getdate", assertKey(props, "allowedMethods", String.class));
        assertEquals("true", assertKey(props, "treatMapAsNamedParams", String.class));
        Map methods = (Map) assertKey(props, "soapMethods", Map.class);
        List method1 = (List) assertKey(methods, "method1", List.class);
        assertEquals(3, method1.size());
        assertEquals("symbol;string;in", method1.get(0));
        assertEquals("GetQuoteResult;string;out", method1.get(1));
        assertEquals("return;string", method1.get(2));
        List method2 = (List) assertKey(methods, "method2", List.class);
        assertEquals(1, method2.size());
        assertEquals("param;string;in", method2.get(0));
        List interfaces = (List) assertKey(props, "serviceInterfaces", List.class);
        assertEquals(2, interfaces.size());
        assertEquals("class1", interfaces.get(0));
        assertEquals("class2", interfaces.get(1));
    }

    protected Object assertKey(Map props, String name, Class clazz)
    {
        assertNotNull(props);
        assertTrue(name + " not in properties", props.containsKey(name));
        Object value = props.get(name);
        assertNotNull(name + " value null", value);
        assertTrue(value.getClass() + " not subclass of " + clazz, clazz.isAssignableFrom(value.getClass()));
        return value; 
    }

}


