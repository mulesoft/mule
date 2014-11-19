/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.cxf;

import static junit.framework.Assert.assertEquals;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.NullPayload;

import java.io.StringReader;
import java.util.List;

import org.apache.cxf.helpers.DOMUtils;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Rule;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

public class ProxyWSDLRewriteAddressTestCase extends FunctionalTestCase
{
    @Rule
    public final DynamicPort httpPort = new DynamicPort("port1");

    private static final String SINGLE_PORT = "StockQuoteSoap";
    private static final String SERVICE_LOCATION = "http://www.webservicex.net/stockquote.asmx";

    @Override
    protected String getConfigFile()
    {
        return "proxy-wsdl-rewrite-address-conf.xml";
    }

    @Test
    public void testProxyWSDLRewriteSinglePort() throws Exception
    {
        String proxyAddress = "http://localhost:" + httpPort.getNumber() + "/single";
        MuleMessage response = muleContext.getClient().send(proxyAddress + "?wsdl", new DefaultMuleMessage(NullPayload.getInstance(), muleContext));
        for(Element port : getPorts(getWsdl(response)))
        {
            String location = getLocation(port);
            String portName = port.getAttribute("name");

            if(SINGLE_PORT.equals(portName))
            {
                assertEquals(proxyAddress, location);
            }
            else
            {
                assertEquals(SERVICE_LOCATION, location);
            }
        }
    }

    @Test
    public void testProxyWSDLRewriteAllPorts() throws Exception
    {
        String proxyAddress = "http://localhost:" + httpPort.getNumber() + "/all";
        MuleMessage response = muleContext.getClient().send(proxyAddress + "?wsdl", new DefaultMuleMessage(NullPayload.getInstance(), muleContext));
        for(Element port : getPorts(getWsdl(response)))
        {
            assertEquals(proxyAddress, getLocation(port));
        }
    }

    private Document getWsdl(MuleMessage response) throws Exception
    {
        return XMLUnit.buildTestDocument(new InputSource(new StringReader(response.getPayloadAsString())));
    }

    private List<Element> getPorts(Document wsdl)
    {
        return DOMUtils.findAllElementsByTagName(wsdl.getDocumentElement(), "wsdl:port");
    }

    private String getLocation(Element port)
    {
        return port.getFirstChild().getNextSibling().getAttributes().getNamedItem("location").getNodeValue();
    }

}
