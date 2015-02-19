/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.sxc;

import static org.junit.Assert.assertTrue;
import static org.mule.module.http.api.HttpConstants.Methods.POST;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.module.http.api.client.HttpRequestOptions;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.ByteArrayOutputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;

public class HttpRoutingTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
    {
        return "http-routing-conf.xml";
    }

    @Test
    public void testBasicXPath() throws Exception
    {
        MuleClient client = muleContext.getClient();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(getClass().getResourceAsStream("/purchase-order.xml"), out);

        String address = "http://localhost:" + dynamicPort.getNumber() + "/proxy";
        final HttpRequestOptions httpRequestOptions = newOptions().method(POST.name()).disableStatusCodeValidation().build();
        MuleMessage res = client.send(address, getTestMuleMessage(out.toByteArray()), httpRequestOptions);
        System.out.println(res.getPayloadAsString());
        assertTrue(res.getPayloadAsString().contains("purchaseOrder"));
        assertTrue(res.getPayloadAsString().contains("Alice"));

        out = new ByteArrayOutputStream();
        IOUtils.copy(getClass().getResourceAsStream("/namespace-purchase-order.xml"), out);

        res = client.send(address, getTestMuleMessage(out.toByteArray()), httpRequestOptions);
        System.out.println(res.getPayloadAsString());
        assertTrue(res.getPayloadAsString().contains("purchaseOrder"));
        assertTrue(res.getPayloadAsString().contains("Alice"));
    }
}
