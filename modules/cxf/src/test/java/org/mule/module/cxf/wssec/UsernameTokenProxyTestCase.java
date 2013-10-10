/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf.wssec;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class UsernameTokenProxyTestCase extends AbstractServiceAndFlowTestCase 
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");
    
    public UsernameTokenProxyTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/module/cxf/wssec/cxf-secure-proxy-service.xml, org/mule/module/cxf/wssec/username-token-conf.xml"},
            {ConfigVariant.FLOW, "org/mule/module/cxf/wssec/cxf-secure-proxy-flow.xml, org/mule/module/cxf/wssec/username-token-conf.xml"}
        });
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
