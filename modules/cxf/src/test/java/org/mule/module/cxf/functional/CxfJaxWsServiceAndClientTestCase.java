/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.functional;


import static org.junit.Assert.assertEquals;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.module.http.api.HttpConstants;
import org.mule.module.http.api.client.HttpRequestOptions;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class CxfJaxWsServiceAndClientTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort port = new DynamicPort("port");

    @Parameterized.Parameter(0)
    public String config;

    private static final HttpRequestOptions HTTP_REQUEST_OPTIONS = newOptions().method(HttpConstants.Methods.POST.name()).build();

    private static final String REQUEST_PAYLOAD =
            "<soap:Envelope \n" +
            "           xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
            "           xmlns:svc=\"http://example.cxf.module.mule.org/\">\n" +
            "<soap:Body>\n" +
            "<svc:sayHi>\n" +
            "    <arg0>Test Message</arg0>\n" +
            "</svc:sayHi>\n" +
            "</soap:Body>\n" +
            "</soap:Envelope>";

    private static final String RESPONSE_PAYLOAD =
            "<soap:Envelope " +
                "xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                "<soap:Body>" +
                    "<ns2:sayHiResponse xmlns:ns2=\"http://example.cxf.module.mule.org/\">" +
                        "<return>" +
                            "Hello\u2297 Test Message" +
                        "</return>" +
                    "</ns2:sayHiResponse>" +
                "</soap:Body>" +
            "</soap:Envelope>";

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {"cxf-jaxws-service-and-client-config.xml"},
                {"cxf-jaxws-service-and-client-config-httpn.xml"}
        });
    }

    @Override
    protected String getConfigFile()
    {
        return config;
    }

    @Test
    public void jaxWsClientReadsMuleMethodPropertySetByJaxWsService() throws Exception
    {
        String url = "http://localhost:" + port.getNumber() + "/hello";
        MuleClient client = muleContext.getClient();

        MuleMessage result = client.send(url, getTestMuleMessage(REQUEST_PAYLOAD), HTTP_REQUEST_OPTIONS);

        assertEquals(RESPONSE_PAYLOAD, result.getPayloadAsString());
    }
}
