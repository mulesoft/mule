/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.wssec;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.InputStream;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class UsernameTokenProxyTestCase extends FunctionalTestCase 
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");
    
    @Override
    protected String getConfigResources() 
    {
        return "org/mule/module/cxf/wssec/cxf-secure-proxy.xml, org/mule/module/cxf/wssec/username-token-conf.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        ClientPasswordCallback.setPassword("secret");        
        super.doSetUp();
    }

    @Test
    public void testProxyEnvelope() throws Exception 
    {
        MuleMessage result = sendRequest("http://localhost:" + dynamicPort.getNumber() + "/proxy-envelope");
        System.out.println(result.getPayloadAsString());
        assertFalse(result.getPayloadAsString().contains("Fault"));
        assertTrue(result.getPayloadAsString().contains("joe"));
    }

    @Test
    public void testProxyBody() throws Exception
    {
        MuleMessage result = sendRequest("http://localhost:" + dynamicPort.getNumber() + "/proxy-body");

        System.out.println(result.getPayloadAsString());
        assertFalse(result.getPayloadAsString().contains("Fault"));
        assertFalse(result.getPayloadAsString().contains("joe"));
    }

    protected MuleMessage sendRequest(String url) throws MuleException
    {
        MuleClient client = new MuleClient(muleContext);

        InputStream stream = getClass().getResourceAsStream(getMessageResource());
        assertNotNull(stream);

        MuleMessage result = client.send(url, new DefaultMuleMessage(stream, muleContext));
        return result;
    }

    protected String getMessageResource()
    {
        return "/org/mule/module/cxf/wssec/in-message.xml";
    }

}
