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
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameter;

public abstract class AbstractWssCheckTimestampInResponseTestCase extends FunctionalTestCase
{

    protected static final String ECHO_REQUEST_WITH_HEADERS = "<tns:echoWithHeaders xmlns:tns=\"http://consumer.ws.module.mule.org/\">" +
                                                              "<text>Hello</text></tns:echoWithHeaders>";
    
    protected static final String EXPECTED_RESPONSE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                                                      "<ns2:echoWithHeadersResponse xmlns:ns2=\"http://consumer.ws.module.mule.org/\">" +
                                                      "<text>Hello</text>" +
                                                      "</ns2:echoWithHeadersResponse>";
    @Rule
    @Parameter(value = 0)
    public SystemProperty checkTimetampInWssResponse;

    @Parameter(value = 1)
    public String expectedResponse;

    @Parameter(value = 2)
    public static DynamicPort port;

    @Parameter(value = 3)
    public String messageToSend;

    @Rule
    public DynamicPort dynamicPortServerNoTSInResponse = new DynamicPort("portServerNoTSInResponse");

    @Rule
    public DynamicPort dynamicPortServerTSInResponse = new DynamicPort("portServerTSInResponse");

    @ClassRule
    public static DynamicPort dynamicPortTSInResponseTSCheck = new DynamicPort("portTSInResponseTSCheck");

    @ClassRule
    public static DynamicPort dynamicPortNoTSInResponseTSCheck = new DynamicPort("portNoTSInResponseTSCheck");

    @ClassRule
    public static DynamicPort dynamicPortTSInResponseNoTSCheck = new DynamicPort("portTSInResponseNoTSCheck");

    @ClassRule
    public static DynamicPort dynamicPortNoTSInResponseNoTSCheck = new DynamicPort("portNoTSInResponseNoTSCheck");

    protected static final String EXPECTED_ERROR_NO_TIMESTAMP_RESPONSE = "An error was discovered processing the <wsse:Security> header";

    public AbstractWssCheckTimestampInResponseTestCase()
    {
        super();
    }

    @Test
    public void checkResponse() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage(messageToSend, muleContext);
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("http://localhost:" + port.getNumber() + "/in",
                request, newOptions().method(POST.name())
                                     .disableStatusCodeValidation()
                                     .build());
        assertThat(response.getPayloadAsString(), equalTo(expectedResponse));
    }

}