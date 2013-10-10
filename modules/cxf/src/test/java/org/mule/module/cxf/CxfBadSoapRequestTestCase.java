/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConstants;
import org.mule.util.StringUtils;

public class CxfBadSoapRequestTestCase extends AbstractServiceAndFlowTestCase
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    public CxfBadSoapRequestTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }
        
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "soap-request-conf-service.xml"},
            {ConfigVariant.FLOW, "soap-request-conf-flow.xml"}
        });
    }

    @Test
    public void testSoapDocumentError() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        String soapRequest = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                             + "<soap:Body>"
                             + "<ssss xmlns=\"http://www.muleumo.org\">"
                             + "<request xmlns=\"http://www.muleumo.org\">Bad Request</request>"
                             + "</ssss>"
                             + "</soap:Body>" + "</soap:Envelope>";

        MuleMessage reply = client.send("http://localhost:" + dynamicPort.getNumber() + "/services/TestComponent", new DefaultMuleMessage(
            soapRequest, muleContext));

        assertNotNull(reply);
        assertNotNull(reply.getPayload());

        String ct = reply.getInboundProperty(HttpConstants.HEADER_CONTENT_TYPE, StringUtils.EMPTY);
        assertEquals("text/xml; charset=UTF-8", ct);        
        
        Document document = DocumentHelper.parseText(reply.getPayloadAsString());
        List fault = document.selectNodes("//soap:Envelope/soap:Body/soap:Fault/faultcode");

        assertEquals(1, fault.size());
        Element faultCodeElement = (Element) fault.get(0);

        assertEquals("soap:Client", faultCodeElement.getStringValue());

        fault = document.selectNodes("//soap:Envelope/soap:Body/soap:Fault/faultstring");
        assertEquals(1, fault.size());
        Element faultStringElement = (Element) fault.get(0);
        assertEquals("Message part {http://www.muleumo.org}ssss was not recognized.  (Does it exist in service WSDL?)",
            faultStringElement.getStringValue());
    }

}
