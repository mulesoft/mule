/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

import static org.junit.Assert.assertEquals;
import static org.mule.module.http.api.HttpConstants.Methods.POST;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import org.mule.api.MuleMessage;
import org.mule.module.http.api.client.HttpRequestOptions;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class CxfComponentExceptionStrategyTestCase extends AbstractServiceAndFlowTestCase
{
    private static final HttpRequestOptions HTTP_REQUEST_OPTIONS = newOptions().method(POST.name()).disableStatusCodeValidation().build();

    private static final String REQUEST_PAYLOAD =
            "<soap:Envelope \n" +
            "           xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
            "           xmlns:svc=\"http://example.cxf.module.mule.org/\">\n" +
            "<soap:Body>\n" +
            "<svc:##method##>\n" +
            "    <arg0>Test</arg0>\n" +
            "</svc:##method##>\n" +
            "</soap:Body>\n" +
            "</soap:Envelope>";

    private static final String SOAP_FAULT =
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                "<soap:Body>" +
                    "<soap:Fault>" +
                        "<faultcode>soap:Server</faultcode>" +
                        "<faultstring>%s</faultstring>" +
                    "</soap:Fault>" +
                "</soap:Body>" +
            "</soap:Envelope>";

    private static final String CUSTOM_SOAP_FAULT =
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                "<soap:Body>" +
                    "<soap:Fault>" +
                        "<faultcode>soap:Server</faultcode>" +
                        "<faultstring>Cxf Exception Message</faultstring>" +
                        "<detail>" +
                            "<ns1:CustomFault xmlns:ns1=\"http://testmodels.cxf.module.mule.org/\">" +
                                "<ns2:description xmlns:ns2=\"http://testmodels.cxf.module.mule.org\">%s</ns2:description>" +
                            "</ns1:CustomFault>" +
                        "</detail>" +
                    "</soap:Fault>" +
                "</soap:Body>" +
            "</soap:Envelope>";

    public CxfComponentExceptionStrategyTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "exception-strategy-conf-service.xml"},
            {ConfigVariant.FLOW, "exception-strategy-conf-flow.xml"},
            {ConfigVariant.FLOW, "exception-strategy-conf-flow-httpn.xml"}
        });
    }

    @Test
    public void testDefaultComponentExceptionStrategyWithFault() throws Exception
    {
        doTest("CxfWithExceptionStrategy", "testFault", SOAP_FAULT, "Invalid data argument");
    }

    // Test to prove that the CxfComponentExceptionStrategy is not needed anymore to unwrap the Fault, the
    // exception cause is the same with or without the custom exception strategy defined, it is only unwrapped inside of
    // the exception block
    @Test
    public void testDefaultExceptionStrategyWithFault() throws Exception
    {
        doTest("CxfWithDefaultExceptionStrategy", "testFault", SOAP_FAULT, "Invalid data argument");
    }

    @Test
    public void testDefaultComponentExceptionStrategyWithCxfException() throws Exception
    {
        doTest("CxfWithExceptionStrategy", "testCxfException", CUSTOM_SOAP_FAULT, "Custom Exception Message");
    }

    // Test to prove that the CxfComponentExceptionStrategy is not needed anymore to unwrap the Fault, the
    // exception cause is the same with or without the custom exception strategy defined, it is only unwrapped inside of
    // the exception block
    @Test
    public void testDefaultExceptionStrategyWithCxfException() throws Exception
    {
        doTest("CxfWithDefaultExceptionStrategy", "testCxfException", CUSTOM_SOAP_FAULT, "Custom Exception Message");
    }

    @Test
    public void testDefaultComponentExceptionStrategyWithException() throws Exception
    {
        doTest("CxfWithExceptionStrategy", "testNonCxfException", SOAP_FAULT, "Non-Cxf Enabled Exception");
    }

    // Test to prove that the CxfComponentExceptionStrategy is not needed anymore to unwrap the Fault, the
    // exception cause is the same with or without the custom exception strategy defined, it is only unwrapped inside of
    // the exception block
    @Test
    public void testDefaultExceptionStrategyWithException() throws Exception
    {
        doTest("CxfWithDefaultExceptionStrategy", "testNonCxfException", SOAP_FAULT, "Non-Cxf Enabled Exception");
    }

    private void doTest(String path, String soapMethod, String faultTemplate, String faultMessage) throws Exception
    {
        MuleMessage response = muleContext.getClient().send(
                String.format("http://localhost:%d/services/%s", dynamicPort.getNumber(), path),
                getTestMuleMessage(getRequestPayload(soapMethod)), HTTP_REQUEST_OPTIONS);
        assertFault(faultTemplate, response.getPayloadAsString(), faultMessage);

    }

    private String getRequestPayload(String method)
    {
        return REQUEST_PAYLOAD.replaceAll("##method##", method);
    }

    private void assertFault(String faultTemplate, String soapResponse, String faultMessage)
    {
        assertEquals(String.format(faultTemplate, faultMessage), soapResponse);
    }
}
