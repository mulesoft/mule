/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.cxf;

import static org.junit.Assert.assertTrue;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import org.mule.api.MuleMessage;
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
public class ProxyRPCBindingTestCase extends FunctionalTestCase
{
    @Rule
    public final DynamicPort httpPortProxy = new DynamicPort("port1");

    @Rule
    public final DynamicPort httpPortService = new DynamicPort("port2");

    public static final HttpRequestOptions HTTP_REQUEST_OPTIONS = newOptions().method(org.mule.module.http.api.HttpConstants.Methods.POST.name()).build();

    private String getAllRequest;
    private String getAllResponse;

    @Parameterized.Parameter(0)
    public String config;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {"proxy-rpc-binding-conf.xml"},
                {"proxy-rpc-binding-conf-httpn.xml"}
        });
    }

    @Override
    protected String getConfigFile()
    {
        return config;
    }

    @Before
    public void doSetUp() throws Exception
    {
        getAllRequest = IOUtils.getResourceAsString("artistregistry-get-all-request.xml", getClass());
        getAllResponse = IOUtils.getResourceAsString("artistregistry-get-all-response.xml", getClass());
        XMLUnit.setIgnoreWhitespace(true);
    }

    @Test
    public void proxyRPCBodyPayload() throws Exception
    {
        MuleMessage response = muleContext.getClient().send("http://localhost:" + httpPortProxy.getNumber() + "/body", getTestMuleMessage(getAllRequest), HTTP_REQUEST_OPTIONS);
        assertTrue(XMLUnit.compareXML(getAllResponse, response.getPayloadAsString()).identical());
    }

    @Test
    public void proxyRPCBodyEnvelope() throws Exception
    {
        MuleMessage response = muleContext.getClient().send("http://localhost:" + httpPortProxy.getNumber() + "/envelope", getTestMuleMessage(getAllRequest), HTTP_REQUEST_OPTIONS);
        assertTrue(XMLUnit.compareXML(getAllResponse, response.getPayloadAsString()).identical());
    }

}
