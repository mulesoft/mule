/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.soap.axis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.module.cxf.SoapConstants;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.soap.axis.mock.MockAxisServer;
import org.mule.transport.soap.axis.mock.MockProvider;

import java.util.List;
import java.util.Map;

import org.apache.axis.constants.Style;
import org.apache.axis.constants.Use;
import org.junit.Test;

public class AxisNamespaceHandlerTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "axis-namespace-config.xml";
    }

    @Test
    public void testConfig()
    {
        AxisConnector connector =
            (AxisConnector)muleContext.getRegistry().lookupConnector("axisConnector");
        
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

    @Test
    public void testInjectedObjects()
    {
        AxisConnector connector =
            (AxisConnector)muleContext.getRegistry().lookupConnector("axisConnector2");

        assertNotNull(connector);
        assertEquals(MockAxisServer.class, connector.getAxis().getClass());
        assertEquals(MockProvider.class, connector.getClientProvider().getClass());
    }

    @Test
    public void testEndpointProperties() throws Exception
    {
        ImmutableEndpoint endpoint =
                muleContext.getRegistry().lookupEndpointBuilder("endpoint").buildOutboundEndpoint();
        Map props = endpoint.getProperties();
        assertEquals("#[methodNamespace]#[method]", assertKey(props, SoapConstants.SOAP_ACTION_PROPERTY, String.class));
        assertEquals("direct", assertKey(props, SoapConstants.SOAP_ACTION_PROPERTY_CAPS, String.class));
        assertEquals("clientConfig", assertKey(props, "clientConfig", String.class));
        assertEquals(Use.ENCODED_STR, assertKey(props, AxisConnector.USE, String.class));
        assertEquals(Style.DOCUMENT_STR, assertKey(props, AxisConnector.STYLE, String.class));
        assertEquals("value1", assertKey(props, "key1", String.class));
        assertEquals("value2", assertKey(props, "key2", String.class));
        Map options = (Map) assertKey(props, AxisMessageReceiver.AXIS_OPTIONS, Map.class);
        assertEquals(10, options.size());
        assertEquals("value1", assertKey(options, "key1", String.class));
        assertEquals("value2", assertKey(options, "key2", String.class));
        assertEquals("Application", assertKey(options, "scope", String.class));
        assertEquals("echo,getdate", assertKey(options, "allowedMethods", String.class));
        assertEquals("wsdlPortType", assertKey(options, "wsdlPortType", String.class));
        assertEquals("wsdlServiceElement", assertKey(options, "wsdlServiceElement", String.class));
        assertEquals("wsdlTargetNamespace", assertKey(options, "wsdlTargetNamespace", String.class));
        assertEquals("wsdlInputSchema", assertKey(options, "wsdlInputSchema", String.class));
        assertEquals("wsdlSoapActionMode", assertKey(options, "wsdlSoapActionMode", String.class));
        assertEquals("extraClasses", assertKey(options, "extraClasses", String.class));
        Map methods = (Map) assertKey(props, AxisConnector.SOAP_METHODS, Map.class);
        List method1 = (List) assertKey(methods, "method1", List.class);
        assertEquals(3, method1.size());
        assertEquals("symbol;string;IN", method1.get(0));
        assertEquals("GetQuoteResult;string;OUT", method1.get(1));
        assertEquals("return;string", method1.get(2));
        List method2 = (List) assertKey(methods, "method2", List.class);
        assertEquals(2, method2.size());
        assertEquals("param;string;IN", method2.get(0));
        assertEquals("addedFromSpring;string;in", method2.get(1));
        List interfaces = (List) assertKey(props, SoapConstants.SERVICE_INTERFACES, List.class);
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


