/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.wssec;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mule.module.http.api.HttpConstants.Methods.POST;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.http.api.client.HttpRequestOptions;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class UsernameTokenProxyTestCase extends AbstractServiceAndFlowTestCase
{

    private static final HttpRequestOptions HTTP_REQUEST_OPTIONS = newOptions().method(POST.name()).disableStatusCodeValidation().build();

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
            {ConfigVariant.FLOW, "org/mule/module/cxf/wssec/cxf-secure-proxy-flow.xml, org/mule/module/cxf/wssec/username-token-conf.xml"},
            {ConfigVariant.FLOW, "org/mule/module/cxf/wssec/cxf-secure-proxy-flow.xml, org/mule/module/cxf/wssec/username-token-conf.xml"}
        });
    }

    @Override
    protected void doSetUp() throws Exception
    {
        ClientPasswordCallback.setPassword("secret");
        super.doSetUp();
    }

    @Ignore("MULE-6926: Flaky Test")
    @Test
    public void testProxyEnvelope() throws Exception
    {
        MuleMessage result = sendRequest("http://localhost:" + dynamicPort.getNumber() + "/proxy-envelope");
        assertFalse(result.getPayloadAsString().contains("Fault"));
        assertTrue(result.getPayloadAsString().contains("joe"));
    }

    @Ignore("MULE-6926: Flaky Test")
    @Test
    public void testProxyBody() throws Exception
    {
        MuleMessage result = sendRequest("http://localhost:" + dynamicPort.getNumber() + "/proxy-body");

        assertFalse(result.getPayloadAsString().contains("Fault"));
        assertFalse(result.getPayloadAsString().contains("joe"));
    }

    protected MuleMessage sendRequest(String url) throws MuleException
    {
        InputStream stream = getClass().getResourceAsStream(getMessageResource());
        assertNotNull(stream);

        return muleContext.getClient().send(url, new DefaultMuleMessage(stream, muleContext), HTTP_REQUEST_OPTIONS);
    }

    protected String getMessageResource()
    {
        return "/org/mule/module/cxf/wssec/in-message.xml";
    }
}
