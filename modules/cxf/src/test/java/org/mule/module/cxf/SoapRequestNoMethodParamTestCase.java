/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mule.module.http.api.HttpConstants.Methods.POST;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.http.api.client.HttpRequestOptions;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class SoapRequestNoMethodParamTestCase extends FunctionalTestCase
{

    private static final HttpRequestOptions HTTP_REQUEST_OPTIONS = newOptions().method(POST.name()).disableStatusCodeValidation().build();

    private static final String request = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"><soap:Body><receive xmlns=\"http://www.muleumo.org\"><src xmlns=\"http://www.muleumo.org\">Test String</src></receive></soap:Body></soap:Envelope>";
    private static final String response = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><ns1:receiveResponse xmlns:ns1=\"http://services.testmodels.tck.mule.org/\"><ns1:return>Received: null</ns1:return></ns1:receiveResponse></soap:Body></soap:Envelope>";

    @Rule
    public DynamicPort port1 = new DynamicPort("port1");
    
    @Parameter
    public String config;

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {"soap-request-conf-flow.xml"},
                {"soap-request-conf-flow-httpn.xml"}
        });
    }
    
    @Override
    protected String getConfigFile()
    {
        return config;
    }

    @Test
    public void testCXFSoapRequest() throws Exception
    {
        MuleMessage msg = muleContext.getClient().send("http://localhost:" + port1.getValue() + "/services/TestComponent", new DefaultMuleMessage(request, muleContext), HTTP_REQUEST_OPTIONS);

        assertNotNull(msg);
        assertNotNull(msg.getPayload());
        assertEquals(response, msg.getPayloadAsString());
    }
}
