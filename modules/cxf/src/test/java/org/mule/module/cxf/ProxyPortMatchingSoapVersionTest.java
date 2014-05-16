/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

import static org.junit.Assert.assertEquals;
import org.mule.api.MuleEvent;
import org.mule.construct.Flow;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class ProxyPortMatchingSoapVersionTest extends FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port");

    @Override
    protected String getConfigFile()
    {
        return "proxy-port-matching-soap-version-config.xml";
    }

    /*
     * This test sends a SOAP 1.2 request to a proxy service with soapVersion="1.2" and no port specified.
     * The proxy service should use the port from the original WSDL that matches the specified version (instead of
     * just taking the first port found).
     */
    @Test
    public void proxyServiceUsesPortMatchingSoapVersion() throws Exception
    {
        String request = "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:web=\"http://www.webserviceX.NET/\">\n" +
                         "   <soap:Header/>\n" +
                         "   <soap:Body>\n" +
                         "      <web:GetQuote><web:symbol>TEST</web:symbol></web:GetQuote>\n" +
                         "   </soap:Body>\n" +
                         "</soap:Envelope>";

        Flow flow = (Flow) getFlowConstruct("proxy");
        MuleEvent result = flow.process(getTestEvent(request));

        // As there is nothing else after the service, if there were no problems the same contents of the request
        // will be returned.
        assertEquals(request, result.getMessage().getPayloadAsString());
    }
}
