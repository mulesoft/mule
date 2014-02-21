/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.module.cxf.wssec.ClientPasswordCallback;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.util.IOUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class UsernameTokenProxyWithoutMustUnderstandTestCase extends FunctionalTestCase
{
    @Rule
    public final DynamicPort httpPortProxy = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
    {
        return "cxf-proxy-service-without-mustunderstand-flow.xml";
    }

    String request;
    String response;

    @Before
    public void doSetUp() throws Exception
    {
        request = IOUtils.getResourceAsString("in-message-with-mustunderstand.xml",getClass());
        response = IOUtils.getResourceAsString("out-message-with-mustunderstand.xml",getClass());
        ClientPasswordCallback.setPassword("secret");
        super.doSetUp();
        XMLUnit.setIgnoreWhitespace(true);
    }

    @Test
    public void testProxyServiceWithoutMustUnderstand() throws Exception
    {
        MuleMessage replyMessage = sendRequest("http://localhost:" + httpPortProxy.getNumber() + "/proxy-envelope", request);
        assertNotNull(replyMessage);
        assertFalse(replyMessage.getPayloadAsString().contains("Fault"));
        assertTrue(XMLUnit.compareXML(response, replyMessage.getPayload().toString()).identical());
    }

    protected MuleMessage sendRequest(String url,String payload) throws MuleException
    {
        MuleClient client = muleContext.getClient();
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("http.method", "POST");
        MuleMessage replyMessage = client.send(url, payload, props);
        return replyMessage;
    }
}
