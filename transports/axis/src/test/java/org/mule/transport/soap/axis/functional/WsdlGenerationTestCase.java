/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.soap.axis.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.NullPayload;

import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.junit.Rule;
import org.junit.Test;

public class WsdlGenerationTestCase extends FunctionalTestCase
{
    /**
     * The generated proxy names have increasing counter if run from the top-level m2
     * build, can be e.g. $Proxy12. Check optionally for 3 digits to be on the safe
     * side.
     */
    private static final String PROXY_REGEX = "^\\$Proxy(\\d+\\d*\\d*)$";

    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");

    @Rule
    public DynamicPort dynamicPort3 = new DynamicPort("port3");

    private Map<String, Object> properties;

    @Override
    protected String getConfigFile()
    {
        return "axis-wsdl-test.xml";
    }

    public WsdlGenerationTestCase()
    {
        super();
    }

    @Test
    public void testWsdl1() throws Exception
    {
        MuleClient client = muleContext.getClient();

        MuleMessage result = client.send("http://localhost:" + dynamicPort1.getNumber() + "/services/EchoService1?wsdl", getTestMuleMessage(NullPayload.getInstance()));
        assertNotNull(result);
        String wsdl = result.getPayloadAsString();
        Document doc = DocumentHelper.parseText(wsdl);
        assertEquals("http://foo", doc.valueOf("/wsdl:definitions/@targetNamespace"));

        // standalone m2 test run can produce $Proxy0, $Proxy1, $Proxy3, etc.
        assertTrue(doc.valueOf("/wsdl:definitions/wsdl:portType/@name").matches(PROXY_REGEX));

        assertEquals(
            "http://foo",
            doc.valueOf("/wsdl:definitions/wsdl:binding/wsdl:operation[@name='echo']/wsdl:input[@name='echoRequest']/wsdlsoap:body/@namespace"));
        assertEquals(
            "http://foo",
            doc.valueOf("/wsdl:definitions/wsdl:binding/wsdl:operation[@name='echo']/wsdl:output[@name='echoResponse']/wsdlsoap:body/@namespace"));

        assertEquals("EchoService1", doc.valueOf("/wsdl:definitions/wsdl:service/@name"));

        assertEquals("EchoService1", doc.valueOf("/wsdl:definitions/wsdl:service/wsdl:port/@name"));
        assertEquals("http://localhost:" + dynamicPort1.getNumber() + "/services/EchoService1",
            doc.valueOf("/wsdl:definitions/wsdl:service/wsdl:port/wsdlsoap:address/@location"));
    }

    @Test
    public void testWsdl2() throws Exception
    {
        MuleClient client = muleContext.getClient();

        MuleMessage result = client.send("http://localhost:" + dynamicPort2.getNumber() + "/services/EchoService2?wsdl", getTestMuleMessage(NullPayload.getInstance()));
        assertNotNull(result);
        String wsdl = result.getPayloadAsString();
        Document doc = DocumentHelper.parseText(wsdl);
        assertEquals("http://simple.component.api.mule.org", doc.valueOf("/wsdl:definitions/@targetNamespace"));
        assertEquals("mulePortType", doc.valueOf("/wsdl:definitions/wsdl:portType/@name"));
        assertEquals(
            "http://simple.component.api.mule.org",
            doc.valueOf("/wsdl:definitions/wsdl:binding/wsdl:operation[@name='echo']/wsdl:input[@name='echoRequest']/wsdlsoap:body/@namespace"));
        assertEquals(
            "http://simple.component.api.mule.org",
            doc.valueOf("/wsdl:definitions/wsdl:binding/wsdl:operation[@name='echo']/wsdl:output[@name='echoResponse']/wsdlsoap:body/@namespace"));
        assertEquals("muleService", doc.valueOf("/wsdl:definitions/wsdl:service/@name"));
        assertEquals("muleServicePort", doc.valueOf("/wsdl:definitions/wsdl:service/wsdl:port/@name"));
        assertEquals("http://localhost:" + dynamicPort2.getNumber() + "/services/EchoService2",
            doc.valueOf("/wsdl:definitions/wsdl:service/wsdl:port/wsdlsoap:address/@location"));
    }

    @Test
    public void testWsdl3() throws Exception
    {
        MuleClient client = muleContext.getClient();

        MuleMessage result = client.send("http://localhost:" + dynamicPort3.getNumber() + "/services/EchoService3?wsdl", getTestMuleMessage(NullPayload.getInstance()));
        assertNotNull(result);
        String wsdl = result.getPayloadAsString();
        Document doc = DocumentHelper.parseText(wsdl);
        assertEquals("http://foo.com", doc.valueOf("/wsdl:definitions/@targetNamespace"));
        assertEquals("mulePortType1", doc.valueOf("/wsdl:definitions/wsdl:portType/@name"));
        assertEquals(
            "http://foo.com",
            doc.valueOf("/wsdl:definitions/wsdl:binding/wsdl:operation[@name='echo']/wsdl:input[@name='echoRequest']/wsdlsoap:body/@namespace"));
        assertEquals(
            "http://foo.com",
            doc.valueOf("/wsdl:definitions/wsdl:binding/wsdl:operation[@name='echo']/wsdl:output[@name='echoResponse']/wsdlsoap:body/@namespace"));
        assertEquals("muleService1", doc.valueOf("/wsdl:definitions/wsdl:service/@name"));
        assertEquals("muleServicePort1", doc.valueOf("/wsdl:definitions/wsdl:service/wsdl:port/@name"));
        // this port is hardcoded in the wsdl, so not the same as the actual endpoint port
        assertEquals("http://localhost:62083/services/EchoService3",
            doc.valueOf("/wsdl:definitions/wsdl:service/wsdl:port/wsdlsoap:address/@location"));
    }
}
