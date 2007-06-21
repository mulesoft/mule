/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.axis.functional;

import org.mule.extras.client.MuleClient;
import org.mule.providers.http.HttpConnector;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

import java.util.HashMap;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;

public class WsdlGenerationTestCase extends FunctionalTestCase
{
    /**
     * The generated proxy names have increasing counter if run from the top-level m2
     * build, can be e.g. $Proxy12. Check optionally for 3 digits to be on the safe
     * side.
     */
    private static final String PROXY_REGEX = "^\\$Proxy(\\d+\\d*\\d*)$";

    public WsdlGenerationTestCase()
    {
        super.setDisposeManagerPerSuite(true);
    }

    protected String getConfigResources()
    {
        return "axis-wsdl-test.xml";
    }

    public void testWsdl1() throws Exception
    {
        Map props = new HashMap();
        props.put(HttpConnector.HTTP_METHOD_PROPERTY, "GET");
        MuleClient client = new MuleClient();

        UMOMessage result = client.send("http://localhost:8081/services/EchoService1?wsdl", null, props);
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
        assertEquals("http://localhost:8081/services/EchoService1",
            doc.valueOf("/wsdl:definitions/wsdl:service/wsdl:port/wsdlsoap:address/@location"));
    }

    public void testWsdl2() throws Exception
    {
        Map props = new HashMap();
        props.put(HttpConnector.HTTP_METHOD_PROPERTY, "GET");
        MuleClient client = new MuleClient();

        UMOMessage result = client.send("http://localhost:8082/services/EchoService2?wsdl", null, props);
        assertNotNull(result);
        String wsdl = result.getPayloadAsString();
        Document doc = DocumentHelper.parseText(wsdl);
        assertEquals("http://simple.components.mule.org", doc.valueOf("/wsdl:definitions/@targetNamespace"));
        assertEquals("mulePortType", doc.valueOf("/wsdl:definitions/wsdl:portType/@name"));
        assertEquals(
            "http://simple.components.mule.org",
            doc.valueOf("/wsdl:definitions/wsdl:binding/wsdl:operation[@name='echo']/wsdl:input[@name='echoRequest']/wsdlsoap:body/@namespace"));
        assertEquals(
            "http://simple.components.mule.org",
            doc.valueOf("/wsdl:definitions/wsdl:binding/wsdl:operation[@name='echo']/wsdl:output[@name='echoResponse']/wsdlsoap:body/@namespace"));
        assertEquals("muleService", doc.valueOf("/wsdl:definitions/wsdl:service/@name"));
        assertEquals("muleServicePort", doc.valueOf("/wsdl:definitions/wsdl:service/wsdl:port/@name"));
        assertEquals("http://localhost:8082/services/EchoService2",
            doc.valueOf("/wsdl:definitions/wsdl:service/wsdl:port/wsdlsoap:address/@location"));
    }

    public void testWsdl3() throws Exception
    {
        Map props = new HashMap();
        props.put(HttpConnector.HTTP_METHOD_PROPERTY, "GET");
        MuleClient client = new MuleClient();

        UMOMessage result = client.send("http://localhost:8083/services/EchoService3?wsdl", null, props);
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
        assertEquals("http://localhost:8083/services/EchoService3",
            doc.valueOf("/wsdl:definitions/wsdl:service/wsdl:port/wsdlsoap:address/@location"));
    }
}
