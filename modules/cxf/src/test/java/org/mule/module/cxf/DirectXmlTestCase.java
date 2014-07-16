/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.module.xml.stax.StaxSource;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;

import org.apache.cxf.helpers.DOMUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class DirectXmlTestCase extends AbstractServiceAndFlowTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    public DirectXmlTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "direct/direct-xml-conf-service.xml"},
            {ConfigVariant.FLOW, "direct/direct-xml-conf-flow.xml"}
        });
    }

    @Test
    public void testInputStream() throws Exception
    {
        MuleClient client = muleContext.getClient();
        InputStream xml = getClass().getResourceAsStream("/direct/direct-request.xml");
        assertNotNull(xml);

        test(client, xml);
    }

    @Test
    public void testInputStreamWithXslt() throws Exception
    {
        MuleClient client = muleContext.getClient();
        InputStream xml = getClass().getResourceAsStream("/direct/direct-request.xml");
        assertNotNull(xml);

        MuleMessage result = client.send("vm://echoWithTransform", xml, null);
        String resultStr = result.getPayloadAsString();
        assertTrue("echoResponse not found in result: " + resultStr, resultStr.indexOf("echoResponse") != -1);
    }

    private void test(MuleClient client, Object xml) throws MuleException, Exception
    {
        MuleMessage result = client.send("vm://echo", xml, null);
        assertTrue(result.getPayloadAsString().indexOf("echoResponse") != -1);
    }

    @Test
    public void testDom() throws Exception
    {
        MuleClient client = muleContext.getClient();
        InputStream xml = getClass().getResourceAsStream("/direct/direct-request.xml");
        Document dom = DOMUtils.readXml(xml);
        test(client, dom);
    }

    @Test
    public void testDomSource() throws Exception
    {
        MuleClient client = muleContext.getClient();
        InputStream xml = getClass().getResourceAsStream("/direct/direct-request.xml");
        Document dom = DOMUtils.readXml(xml);
        test(client, new DOMSource(dom));
    }

    @Test
    public void testSAXSource() throws Exception
    {
        MuleClient client = muleContext.getClient();
        InputStream xml = getClass().getResourceAsStream("/direct/direct-request.xml");
        SAXSource source = new SAXSource(new InputSource(xml));
        test(client, source);
    }

    @Test
    public void testStaxSource() throws Exception
    {
        MuleClient client = muleContext.getClient();
        InputStream xml = getClass().getResourceAsStream("/direct/direct-request.xml");

        XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(xml);
        test(client, new StaxSource(reader));
    }

    @Test
    public void testXMLStreamReader() throws Exception
    {
        MuleClient client = muleContext.getClient();
        InputStream xml = getClass().getResourceAsStream("/direct/direct-request.xml");

        XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(xml);
        test(client, reader);
    }
}
