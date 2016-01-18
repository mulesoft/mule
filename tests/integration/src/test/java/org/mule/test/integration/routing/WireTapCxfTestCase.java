/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mule.module.http.api.HttpConstants.Methods.POST;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class WireTapCxfTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort port1 = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/routing/wire-tap-cxf-flow.xml";
    }

    @Test
    public void testWireTap() throws Exception
    {
        String url = "http://localhost:" + port1.getNumber() + "/services/EchoUMO";
        String msg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                     + "<soap:Body><echo><text>foo</text></echo></soap:Body></soap:Envelope>";

        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send(url, getTestMuleMessage(msg), newOptions().method(POST.name()).build());
        assertNotNull(response);

        String responseString = getPayloadAsString(response);
        assertTrue(responseString.contains("echoResponse"));
        assertFalse(responseString.contains("soap:Fault"));

        assertNotNull(client.request("test://wireTapped", RECEIVE_TIMEOUT));
    }
}
