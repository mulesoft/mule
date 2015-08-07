/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.runners.Parameterized.Parameter;
import static org.junit.runners.Parameterized.Parameters;
import static org.mule.module.http.api.HttpConstants.Methods.POST;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.cxf.wssec.ClientPasswordCallback;
import org.mule.module.http.api.client.HttpRequestOptions;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.util.IOUtils;

import java.util.Arrays;
import java.util.Collection;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class UsernameTokenProxyWithoutMustUnderstandTestCase extends FunctionalTestCase
{

    private static final HttpRequestOptions HTTP_REQUEST_OPTIONS = newOptions().method(POST.name()).disableStatusCodeValidation().build();

    @Rule
    public final DynamicPort httpPortProxy = new DynamicPort("port1");

    private String request;
    private String response;

    @Parameter
    public String configFile;

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {"cxf-proxy-service-without-mustunderstand-flow.xml"},
                {"cxf-proxy-service-without-mustunderstand-flow-httpn.xml"}
        });
    }

    @Override
    protected String getConfigFile()
    {
        return configFile;
    }

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
        return muleContext.getClient().send(url, getTestMuleMessage(payload), HTTP_REQUEST_OPTIONS);
    }
}
