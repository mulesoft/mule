/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.mule.api.MuleEvent;
import org.mule.construct.Flow;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class ProxyPortMatchingSoapVersionTest extends FunctionalTestCase
{

    private static final String SOAP_REQUEST_PATTERN = "<soap:Envelope xmlns:soap=\"%s\" xmlns:web=\"http://www.webserviceX.NET/\">\n" +
                                                       "   <soap:Header/>\n" +
                                                       "   <soap:Body>\n" +
                                                       "      <web:GetQuote><web:symbol>TEST</web:symbol></web:GetQuote>\n" +
                                                       "   </soap:Body>\n" +
                                                       "</soap:Envelope>";

    private static final String SOAP_11_SCHEMA = "http://schemas.xmlsoap.org/soap/envelope/";

    private static final String SOAP_12_SCHEMA = "http://www.w3.org/2003/05/soap-envelope";

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port");

    @Override
    protected String getConfigFile()
    {
        return "proxy-port-matching-soap-version-config.xml";
    }

    /*
     * Proxy service with soapVersion="1.1", no port, and a WSDL that has multiple ports (1.1 and 1.2).
     * Service should bind to 1.1 port.
     */
    @Test
    public void proxyServiceMultiplePortsSoapVersion11() throws Exception
    {
        assertValidRequest("proxy11_multiplePorts", getRequest(SOAP_11_SCHEMA));
        assertInvalidRequest("proxy11_multiplePorts", getRequest(SOAP_12_SCHEMA));
    }

    /*
     * Proxy service with soapVersion="1.2", no port, and a WSDL that has multiple ports (1.1 and 1.2).
     * Service should bind to 1.2 port.
     */
    @Test
    public void proxyServiceMultiplePortsSoapVersion12() throws Exception
    {
        assertValidRequest("proxy12_multiplePorts", getRequest(SOAP_12_SCHEMA));
    }

    /*
     * Proxy service with soapVersion="1.2", no port, and a WSDL that has a single 1.1 port.
     * Service should bind to the 1.1 port (to keep previous behaviour).
     */
    @Test
    public void proxyServiceSinglePortSoapVersion12() throws Exception
    {
        assertValidRequest("proxy12_singlePort", getRequest(SOAP_11_SCHEMA));
        assertInvalidRequest("proxy12_singlePort", getRequest(SOAP_12_SCHEMA));
    }

    private void assertValidRequest(String flowName, String request) throws Exception
    {
        Flow flow = (Flow) getFlowConstruct(flowName);
        MuleEvent result = flow.process(getTestEvent(request));

        // As there is nothing else after the service, if there were no problems the same contents of the request
        // will be returned.
        assertEquals(request, result.getMessage().getPayloadAsString());
    }

    private void assertInvalidRequest(String flowName, String request) throws Exception
    {
        Flow flow = (Flow) getFlowConstruct(flowName);
        MuleEvent result = flow.process(getTestEvent(request));

        assertTrue(result.getMessage().getPayloadAsString().contains("VersionMismatch"));
    }

    private String getRequest(String schema)
    {
        return String.format(SOAP_REQUEST_PATTERN, schema);
    }
}
