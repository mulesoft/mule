/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.xml;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConnector;

import java.io.InputStream;

import org.junit.Rule;
import org.junit.Test;

public class XmlSendTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/xml/xml-conf-flow.xml";
    }

	@Test
    public void testXmlFilter() throws Exception
    {
        InputStream xml = getClass().getResourceAsStream("request.xml");

        assertNotNull(xml);

        MuleClient client = muleContext.getClient();

        // this will submit the xml via a POST request
        MuleMessage message = client.send("http://localhost:" + dynamicPort.getNumber() + "/xml-parse", getTestMuleMessage(xml));
        assertEquals("200", message.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY));

        // This won't pass the filter
        xml = getClass().getResourceAsStream("validation1.xml");
        message = client.send("http://localhost:" + dynamicPort.getNumber() + "/xml-parse", getTestMuleMessage(xml));
        assertEquals("406", message.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY));
    }

	@Test
    public void testXmlFilterAndXslt() throws Exception
    {
        InputStream xml = getClass().getResourceAsStream("request.xml");

        assertNotNull(xml);

        MuleClient client = muleContext.getClient();

        // this will submit the xml via a POST request
        MuleMessage message = client.send("http://localhost:" + dynamicPort.getNumber() + "/xml-xslt-parse", getTestMuleMessage(xml));
        assertEquals("200", message.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY));
    }

	@Test
    public void testXmlValidation() throws Exception
    {
        InputStream xml = getClass().getResourceAsStream("validation1.xml");

        assertNotNull(xml);

        MuleClient client = muleContext.getClient();

        // this will submit the xml via a POST request
        MuleMessage message = client.send("http://localhost:" + dynamicPort.getNumber() + "/validate", getTestMuleMessage(xml));
        assertEquals("200", message.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY));

        xml = getClass().getResourceAsStream("validation2.xml");
        message = client.send("http://localhost:" + dynamicPort.getNumber() + "/validate", getTestMuleMessage(xml));
        assertEquals("406", message.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY));

        xml = getClass().getResourceAsStream("validation3.xml");
        message = client.send("http://localhost:" + dynamicPort.getNumber() + "/validate", getTestMuleMessage(xml));
        assertEquals("200", message.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY));
    }

    @Test
    public void testExtractor() throws Exception
    {
        InputStream xml = getClass().getResourceAsStream("validation1.xml");
        MuleClient client = muleContext.getClient();

        // this will submit the xml via a POST request
        MuleMessage message = client.send("http://localhost:" + dynamicPort.getNumber() + "/extract", getTestMuleMessage(xml));
        assertThat(message.getPayloadAsString(), equalTo("some"));
    }
}
