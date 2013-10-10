/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf;

import static org.junit.Assert.assertTrue;
import org.mule.api.MuleMessage;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class ProxyWithValidationTestCase extends FunctionalTestCase
{

    public static final String SAMPLE_REQUEST = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                                                + "<soap:Body> " +
                                                "<echo xmlns=\"http://www.muleumo.org\">" +
                                                "  <echo><![CDATA[bla]]></echo>" +
                                                "</echo>"
                                                + "</soap:Body>"
                                                + "</soap:Envelope>";

    @Rule
    public final DynamicPort httpPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "proxy-with-validation-config.xml";
    }

    @Test
    public void acceptsRequestWithCData() throws Exception
    {
        MuleMessage response = muleContext.getClient().send("http://localhost:" + httpPort.getNumber(), SAMPLE_REQUEST, null);

        assertTrue(response.getPayloadAsString().contains("bla"));
    }
}
