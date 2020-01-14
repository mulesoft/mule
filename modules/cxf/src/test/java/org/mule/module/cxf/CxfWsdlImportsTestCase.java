/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.runners.Parameterized.Parameter;
import static org.junit.runners.Parameterized.Parameters;

import org.mule.api.MuleMessage;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class CxfWsdlImportsTestCase extends FunctionalTestCase
{

    private static final String VALID_REQUEST_ECHO = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                                                     + "<soap:Body> " +
                                                     "<echo xmlns=\"http://www.echo.org\">" +
                                                     "<echo>Testing echo</echo>" +
                                                     "</echo>"
                                                     + "</soap:Body>"
                                                     + "</soap:Envelope>";

    private static final String INVALID_REQUEST_ECHO = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                                                       + "<soap:Body> " +
                                                       "<wrongEcho xmlns=\"http://www.echo.org\">" +
                                                       "  <echo>Testing echo</ech>" +
                                                       "</wrongEcho>"
                                                       + "</soap:Body>"
                                                       + "</soap:Envelope>";

    private static final String VALID_REQUEST_LOG = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:log=\"http://www.log.org\">" +
                                                    "<soapenv:Body>" +
                                                    "<log:log>" +
                                                    "<log:message>Testing Log</log:message>" +
                                                    "</log:log>" +
                                                    "</soapenv:Body>" +
                                                    "</soapenv:Envelope>";

    private static final String INVALID_REQUEST_LOG = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:log=\"http://www.muleumo.org\">" +
                                                      "<soapenv:Body>" +
                                                      "<log:wrongLog>" +
                                                      "<log:message>Testing Log</log:message>" +
                                                      "</log:wrongLog>" +
                                                      "</soapenv:Body>" +
                                                      "</soapenv:Envelope>";

    private static final String ECHO_TEST_ERROR_MESSAGE = "tag name \"wrongEcho\" is not allowed";

    private static final String LOG_TEST_ERROR_MESSAGE = "tag name \"wrongLog\" is not allowed";
    
    private static final String VALID_RESPONSE_LOG = "<ns1:logResponse xmlns:ns1=\"http://www.log.org\"/>";

    private static final Object VALID_RESPONSE_ECHO = "<ns1:echoResponse xmlns:ns1=\"http://www.echo.org\">"
                                                     + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                                                     + "<soap:Body> " +
                                                     "<echo xmlns=\"http://www.echo.org\">" +
                                                     "<echo>Testing echo</echo>" +
                                                     "</echo>"
                                                     + "</soap:Body>"
                                                     + "</soap:Envelope>"
                                                     + "</ns1:echoResponse>";

    @Rule
    public final DynamicPort httpPort = new DynamicPort("port1");

    @Parameter
    public String validPayload;

    @Parameter(1)
    public String invalidPayload;

    @Parameter(2)
    public String errorMessage;
    
    @Parameter(3)
    public String validResponse;

    @Parameters
    public static Collection<Object[]> data()
    {
        return Arrays.asList(new Object[][]
                                     {
                                             {VALID_REQUEST_ECHO, INVALID_REQUEST_ECHO, ECHO_TEST_ERROR_MESSAGE, VALID_RESPONSE_ECHO},
                                             {VALID_REQUEST_LOG, INVALID_REQUEST_LOG, LOG_TEST_ERROR_MESSAGE, VALID_RESPONSE_LOG}
                                     });
    }


    @Override
    protected String getConfigFile()
    {
        return "proxy-validation-imports-config.xml";
    }

    @Test
    public void testValidRequest() throws Exception
    {
        MuleMessage response = muleContext.getClient().send("http://localhost:" + httpPort.getNumber() + "/services/test", getTestMuleMessage(validPayload));
        assertThat(response.getPayloadAsString(), is(validResponse));
    }

    @Test
    public void testInvalidRequest() throws Exception
    {
        MuleMessage response = muleContext.getClient().send("http://localhost:" + httpPort.getNumber() + "/services/test", getTestMuleMessage(invalidPayload));
        assertThat(response.getPayloadAsString(), containsString(errorMessage));
    }

}
