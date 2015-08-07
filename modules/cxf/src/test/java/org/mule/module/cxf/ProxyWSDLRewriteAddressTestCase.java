/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.cxf;

import static junit.framework.Assert.assertEquals;
import static org.junit.runners.Parameterized.Parameter;
import static org.junit.runners.Parameterized.Parameters;
import static org.mule.module.http.api.HttpConstants.Methods.POST;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import org.mule.api.MuleMessage;
import org.mule.module.http.api.client.HttpRequestOptions;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.NullPayload;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.cxf.helpers.DOMUtils;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

@RunWith(Parameterized.class)
public class ProxyWSDLRewriteAddressTestCase extends FunctionalTestCase
{

    private static final HttpRequestOptions HTTP_REQUEST_OPTIONS = newOptions().method(POST.name()).disableStatusCodeValidation().build();

    @Rule
    public final DynamicPort httpPort = new DynamicPort("port1");

    private static final String SINGLE_PORT = "StockQuoteSoap";
    private static final String SERVICE_LOCATION = "http://www.webservicex.net/stockquote.asmx";

    @Parameter
    public String configFile;

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {"proxy-wsdl-rewrite-address-conf.xml"},
                {"proxy-wsdl-rewrite-address-conf-httpn.xml"}
        });
    }

    @Override
    protected String getConfigFile()
    {
        return configFile;
    }

    @Test
    public void testProxyWSDLRewriteSinglePort() throws Exception
    {
        String proxyAddress = "http://localhost:" + httpPort.getNumber() + "/single";
        MuleMessage response = muleContext.getClient().send(proxyAddress + "?wsdl", getTestMuleMessage(NullPayload.getInstance()), HTTP_REQUEST_OPTIONS);
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
        MuleMessage response = muleContext.getClient().send(proxyAddress + "?wsdl", getTestMuleMessage(NullPayload.getInstance()), HTTP_REQUEST_OPTIONS);
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
