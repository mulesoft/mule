/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.module.http.api.client.HttpRequestOptions;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConstants;
import org.mule.util.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class CxfBadSoapRequestTestCase extends AbstractServiceAndFlowTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    private static final HttpRequestOptions HTTP_REQUEST_OPTIONS = newOptions().method(org.mule.module.http.api.HttpConstants.Methods.POST.name()).disableStatusCodeValidation().build();

    public CxfBadSoapRequestTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "soap-request-conf-service.xml"},
            {ConfigVariant.FLOW, "soap-request-conf-flow.xml"},
            {ConfigVariant.FLOW, "soap-request-conf-flow-httpn.xml"}
        });
    }

    @Test
    public void testSoapDocumentError() throws Exception
    {
        MuleClient client = muleContext.getClient();

        String soapRequest = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                             + "<soap:Body>"
                             + "<ssss xmlns=\"http://www.muleumo.org\">"
                             + "<request xmlns=\"http://www.muleumo.org\">Bad Request</request>"
                             + "</ssss>"
                             + "</soap:Body>" + "</soap:Envelope>";

        MuleMessage reply = client.send("http://localhost:" + dynamicPort.getNumber() + "/services/TestComponent", getTestMuleMessage(
            soapRequest), HTTP_REQUEST_OPTIONS);

        assertNotNull(reply);
        assertNotNull(reply.getPayload());

        String ct = reply.getInboundProperty(HttpConstants.HEADER_CONTENT_TYPE, StringUtils.EMPTY);
        assertEquals("text/xml; charset=UTF-8", ct);

        Document document = DocumentHelper.parseText(reply.getPayloadAsString());
        List<?> fault = document.selectNodes("//soap:Envelope/soap:Body/soap:Fault/faultcode");

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
