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
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.config.i18n.LocaleMessageHandler;
import org.mule.module.http.api.client.HttpRequestOptions;
import org.mule.module.http.api.client.HttpRequestOptionsBuilder;
import org.mule.module.xml.util.XMLUtils;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConstants;
import org.mule.util.IOUtils;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.xml.transform.TransformerFactoryConfigurationError;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class CxfBasicTestCase extends AbstractServiceAndFlowTestCase
{
    private String echoWsdl;

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    public CxfBasicTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "basic-conf-service.xml"},
            {ConfigVariant.FLOW, "basic-conf-flow.xml"}
        });
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
    public void testEchoService() throws Exception
    {
        MuleClient client = muleContext.getClient();
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("Content-Type", "application/soap+xml");
        InputStream xml = getClass().getResourceAsStream("/direct/direct-request.xml");
        final HttpRequestOptions httpRequestOptions = newOptions().method(org.mule.module.http.api.HttpConstants.Methods.POST).build();
        MuleMessage result = client.send(((InboundEndpoint) muleContext.getRegistry()
            .lookupObject("httpInbound")).getAddress(), new DefaultMuleMessage(xml, props, muleContext), httpRequestOptions);
        assertTrue(result.getPayloadAsString().contains("Hello!"));
        String ct = result.getInboundProperty(HttpConstants.HEADER_CONTENT_TYPE, "");
        assertEquals("text/xml; charset=UTF-8", ct);
    }

    @Test
    public void testEchoServiceEncoding() throws Exception
    {
        MuleClient client = muleContext.getClient();
        String message = LocaleMessageHandler.getString("test-data",
            Locale.JAPAN, "CxfBasicTestCase.testEchoServiceEncoding", new Object[]{});
        MuleMessage result = client.send("cxf:" + ((InboundEndpoint) muleContext.getRegistry()
                        .lookupObject("httpInbound")).getAddress() + "?method=echo", new DefaultMuleMessage(message, muleContext));
        String ct = result.getInboundProperty(HttpConstants.HEADER_CONTENT_TYPE, "");

        assertEquals(message, result.getPayload());
        assertEquals("text/xml; charset=UTF-8", ct);
    }

    @Test
    public void testEchoWsdl() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage result = client.request(((InboundEndpoint) muleContext.getRegistry()
                        .lookupObject("httpInbound")).getAddress() + "?wsdl", 5000);
        assertNotNull(result.getPayload());
        XMLUnit.compareXML(echoWsdl, result.getPayloadAsString());
    }
}
