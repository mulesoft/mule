/*
 * $Id: CxfBasicTestCase.java 11405 2008-03-18 00:13:00Z dirk.olmes $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.module.xml.stax.StaxSource;
import org.mule.tck.FunctionalTestCase;

import java.io.InputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;

import org.apache.cxf.helpers.DOMUtils;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class DirectXmlTestCase extends FunctionalTestCase
{
    public void testInputStream() throws Exception
    {
        MuleClient client = new MuleClient();
        InputStream xml = getClass().getResourceAsStream("/direct/direct-request.xml");
        assertNotNull(xml);
        
        test(client, xml);
    }
    
    public void testInputStreamWithXslt() throws Exception
    {
        MuleClient client = new MuleClient();
        InputStream xml = getClass().getResourceAsStream("/direct/direct-request.xml");
        assertNotNull(xml);

        MuleMessage result = client.send("vm://echoWithTransform", xml, null);
        String resultStr = result.getPayloadAsString();        
        assertTrue("echoResponse not found in result: " + resultStr, resultStr.indexOf("echoResponse") != -1);
    }
    
    private void test(MuleClient client, Object xml) throws MuleException, Exception
    {
        MuleMessage result = client.send("vm://echo", 
            xml,
            null);
        
//        System.out.println(result.getPayloadAsString()); 
        assertTrue(result.getPayloadAsString().indexOf("echoResponse") != -1);
    }
    
    public void testDom() throws Exception
    {
        MuleClient client = new MuleClient();
        InputStream xml = getClass().getResourceAsStream("/direct/direct-request.xml");
        Document dom = DOMUtils.readXml(xml);
        test(client, dom);
    }

    public void testDomSource() throws Exception
    {
        MuleClient client = new MuleClient();
        InputStream xml = getClass().getResourceAsStream("/direct/direct-request.xml");
        Document dom = DOMUtils.readXml(xml);
        test(client, new DOMSource(dom));
    }

    public void testSAXSource() throws Exception
    {
        MuleClient client = new MuleClient();
        InputStream xml = getClass().getResourceAsStream("/direct/direct-request.xml");
        SAXSource source = new SAXSource(new InputSource(xml));
        test(client, source);
    }
    
    public void testStaxSource() throws Exception
    {
        MuleClient client = new MuleClient();
        InputStream xml = getClass().getResourceAsStream("/direct/direct-request.xml");
        
        XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(xml);
        test(client, new StaxSource(reader));
    }
    
    public void testXMLStreamReader() throws Exception
    {
        MuleClient client = new MuleClient();
        InputStream xml = getClass().getResourceAsStream("/direct/direct-request.xml");

        XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(xml);
        test(client, reader);
    }

    protected String getConfigResources()
    {
        return "direct/direct-xml-conf.xml";
    }

}
