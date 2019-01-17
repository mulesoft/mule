/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.functional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpConstants.Methods.POST;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class WssTimestampInResponseTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPortServerNoTSInResponse = new DynamicPort("portServerNoTSInResponse");
    
    @Rule
    public DynamicPort dynamicPortServerTSInResponse = new DynamicPort("portServerTSInResponse");

    @Rule
    public DynamicPort dynamicPortTSInResponseTSCheck = new DynamicPort("portTSInResponseTSCheck");

    @Rule
    public DynamicPort dynamicPortNoTSInResponseTSCheck = new DynamicPort("portNoTSInResponseTSCheck");

    @Rule
    public DynamicPort dynamicPortTSInResponseNoTSCheck = new DynamicPort("portTSInResponseNoTSCheck");
    
    @Rule
    public DynamicPort dynamicPortNoTSInResponseNoTSCheck = new DynamicPort("portNoTSInResponseNoTSCheck");

    private static final String ECHO_REQUEST_WITH_HEADERS = "<tns:echoWithHeaders xmlns:tns=\"http://consumer.ws.module.mule.org/\">" +
                                                            "<text>Hello</text></tns:echoWithHeaders>";

    protected static final String EXPECTED_RESPONSE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                                                      "<ns2:echoWithHeadersResponse xmlns:ns2=\"http://consumer.ws.module.mule.org/\">" +
                                                      "<text>Hello</text>" +
                                                      "</ns2:echoWithHeadersResponse>";

    protected static final String EXPECTED_ERROR_NO_TIMESTAMP_RESPONSE = "An error was discovered processing the <wsse:Security> header";

    @Override
    protected String getConfigFile()
    {
        return "wss-timestamp-in-response-test-case.xml";
    }

    @Test
    public void checkTSInResponseAndTSInResponseReturnsEchoMessage() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage(ECHO_REQUEST_WITH_HEADERS, muleContext);
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("http://localhost:" + dynamicPortTSInResponseTSCheck.getNumber() + "/in",
                request, newOptions().method(POST.name())
                                     .disableStatusCodeValidation()
                                     .build());
        assertThat(response.getPayloadAsString(), equalTo(EXPECTED_RESPONSE));
    }

    @Test
    public void noCheckTSInResponseAndTSInResponseReturnsErrorMessage() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage(ECHO_REQUEST_WITH_HEADERS, muleContext);
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("http://localhost:" + dynamicPortTSInResponseNoTSCheck.getNumber() + "/in",
                request, newOptions().method(POST.name())
                                     .disableStatusCodeValidation()
                                     .build());
        assertThat(response.getPayloadAsString(), equalTo(EXPECTED_ERROR_NO_TIMESTAMP_RESPONSE));
    }

    @Test
    public void checkTSInResponseAndNoTSInResponseReturnsErrorMessage() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage(ECHO_REQUEST_WITH_HEADERS, muleContext);
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("http://localhost:" + dynamicPortNoTSInResponseTSCheck.getNumber() + "/in",
                request, newOptions().method(POST.name())
                                     .disableStatusCodeValidation()
                                     .build());
        assertThat(response.getPayloadAsString(), equalTo(EXPECTED_ERROR_NO_TIMESTAMP_RESPONSE));
    }
    
    @Test
    public void noCheckTSInResponseAndNoTSInResponseReturnsEchoMessage() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage(ECHO_REQUEST_WITH_HEADERS, muleContext);
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("http://localhost:" + dynamicPortNoTSInResponseNoTSCheck.getNumber() + "/in",
                request, newOptions().method(POST.name())
                                     .disableStatusCodeValidation()
                                     .build());
        assertThat(response.getPayloadAsString(), equalTo(EXPECTED_RESPONSE));
    }

    

}
