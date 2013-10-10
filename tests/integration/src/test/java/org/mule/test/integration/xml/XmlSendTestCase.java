/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.xml;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConnector;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class XmlSendTestCase extends AbstractServiceAndFlowTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

	@Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/integration/xml/xml-conf-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/integration/xml/xml-conf-flow.xml"}
        });
    }

    public XmlSendTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

	@Test
    public void testXmlFilter() throws Exception
    {
        InputStream xml = getClass().getResourceAsStream("request.xml");

        assertNotNull(xml);

        MuleClient client = new MuleClient(muleContext);

        // this will submit the xml via a POST request
        MuleMessage message = client.send("http://localhost:" + dynamicPort.getNumber() + "/xml-parse", xml, null);
        assertEquals("200", message.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY));

        // This won't pass the filter
        xml = getClass().getResourceAsStream("validation1.xml");
        message = client.send("http://localhost:" + dynamicPort.getNumber() + "/xml-parse", xml, null);
        assertEquals("406", message.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY));
    }

	@Test
    public void testXmlFilterAndXslt() throws Exception
    {
        InputStream xml = getClass().getResourceAsStream("request.xml");

        assertNotNull(xml);

        MuleClient client = new MuleClient(muleContext);

        // this will submit the xml via a POST request
        MuleMessage message = client.send("http://localhost:" + dynamicPort.getNumber() + "/xml-xslt-parse", xml, null);
        assertEquals("200", message.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY));
    }

	@Test
    public void testXmlValidation() throws Exception
    {
        InputStream xml = getClass().getResourceAsStream("validation1.xml");

        assertNotNull(xml);

        MuleClient client = new MuleClient(muleContext);

        // this will submit the xml via a POST request
        MuleMessage message = client.send("http://localhost:" + dynamicPort.getNumber() + "/validate", xml, null);
        assertEquals("200", message.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY));

        xml = getClass().getResourceAsStream("validation2.xml");
        message = client.send("http://localhost:" + dynamicPort.getNumber() + "/validate", xml, null);
        assertEquals("406", message.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY));

        xml = getClass().getResourceAsStream("validation3.xml");
        message = client.send("http://localhost:" + dynamicPort.getNumber() + "/validate", xml, null);
        assertEquals("200", message.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY));
    }

}
