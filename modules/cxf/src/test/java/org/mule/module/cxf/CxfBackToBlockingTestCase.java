/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mule.module.http.api.HttpConstants.Methods;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.module.http.api.client.HttpRequestOptions;
import org.mule.module.xml.util.XMLUtils;
import org.mule.tck.SensingNullRequestResponseMessageProcessor;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConstants;
import org.mule.util.IOUtils;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.TransformerFactoryConfigurationError;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Rule;
import org.junit.Test;

public class CxfBackToBlockingTestCase extends FunctionalTestCase
{

    private static final HttpRequestOptions HTTP_REQUEST_OPTIONS = newOptions().method(Methods.POST.name()).build();

    private String echoWsdl;

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
    {
        return "basic-conf-flow-httpn-nb.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        echoWsdl = IOUtils.getResourceAsString("cxf-echo-service.wsdl", getClass());
        XMLUnit.setIgnoreWhitespace(true);
        try
        {
            XMLUnit.getTransformerFactory();
        }
        catch (TransformerFactoryConfigurationError e)
        {
            XMLUnit.setTransformerFactory(XMLUtils.TRANSFORMER_FACTORY_JDK5);
        }
    }

    @Test
    public void backToBlocking() throws Exception
    {
        MuleClient client = muleContext.getClient();
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("Content-Type", "application/soap+xml");
        InputStream xml = getClass().getResourceAsStream("/direct/direct-request.xml");
        MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/services/Echo", new DefaultMuleMessage(xml, props, muleContext), HTTP_REQUEST_OPTIONS);
        assertTrue(result.getPayloadAsString().contains("Hello!"));
        String ct = result.getInboundProperty(HttpConstants.HEADER_CONTENT_TYPE, "");
        assertEquals("text/xml; charset=UTF-8", ct);
        muleContext.getRegistry().lookupObject(SensingNullRequestResponseMessageProcessor.class).assertRequestResponseThreadsSame();
    }

    @Test
    public void backToBlockingWsdl() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/services/Echo" + "?wsdl", getTestMuleMessage(null), HTTP_REQUEST_OPTIONS);
        assertNotNull(result.getPayload());
        XMLUnit.compareXML(echoWsdl, result.getPayloadAsString());
        muleContext.getRegistry().lookupObject(SensingNullRequestResponseMessageProcessor.class).assertRequestResponseThreadsSame();
    }

}
