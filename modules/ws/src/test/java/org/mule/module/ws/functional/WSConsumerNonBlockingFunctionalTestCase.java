/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.ws.functional;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpConstants.Methods.POST;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.SensingNullRequestResponseMessageProcessor;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class WSConsumerNonBlockingFunctionalTestCase extends AbstractWSConsumerFunctionalTestCase
{

    @Parameterized.Parameter(value = 1)
    public String configFile;

    @Override
    protected String getConfigFile()
    {
        return configFile;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {{true, "ws-consumer-http-module-config-nb.xml"}});
    }

    @Test
    public void validRequestReturnsExpectedAnswer() throws Exception
    {
        assertValidResponse("http://localhost:" + dynamicPort.getNumber() + "/in");
        muleContext.getRegistry().lookupObject(SensingNullRequestResponseMessageProcessor.class).assertRequestResponseThreadsDifferent();
    }

    @Test
    public void invalidRequestFormatReturnsSOAPFault() throws Exception
    {
        String message = "<tns:echo xmlns:tns=\"http://consumer.ws.module.mule.org/\"><invalid>Hello</invalid></tns:echo>";
        assertSoapFault("http://localhost:" + dynamicPort.getNumber() + "/in", message, "unexpected element (uri:\"\", local:\"invalid\")");
        muleContext.getRegistry().lookupObject(SensingNullRequestResponseMessageProcessor.class).assertRequestResponseThreadsDifferent();
    }

    @Test
    public void invalidNamespaceReturnsSOAPFault() throws Exception
    {
        String message = "<tns:echo xmlns:tns=\"http://invalid/\"><text>Hello</text></tns:echo>";
        assertSoapFault("http://localhost:" + dynamicPort.getNumber() + "/in", message, "Unexpected wrapper element {http://invalid/}echo found");
        muleContext.getRegistry().lookupObject(SensingNullRequestResponseMessageProcessor.class).assertRequestResponseThreadsDifferent();
    }

    @Test
    public void webServiceConsumerMidFlow() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage(ECHO_REQUEST, muleContext);
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("http://localhost:" + dynamicPort.getNumber() + "/inMidFlow",
                                           request, newOptions().method(POST.name()).disableStatusCodeValidation()
                                                   .build());
        assertThat(response.getPayloadAsString(), equalTo(TEST_MESSAGE));
    }

    protected void assertValidResponse(String address, Map<String, Object> properties) throws Exception
    {
        MuleMessage request = new DefaultMuleMessage(ECHO_REQUEST, properties, muleContext);
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send(address, request, newOptions().method(POST.name()).disableStatusCodeValidation().build());
        assertXMLEqual(EXPECTED_ECHO_RESPONSE, response.getPayloadAsString());
    }

    protected void assertSoapFault(String address, String message, Map<String, Object> properties, String expectedErrorMessage) throws Exception
    {
        MuleMessage request = new DefaultMuleMessage(message, properties, muleContext);
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send(address, request, newOptions().method(POST.name()).disableStatusCodeValidation().build());
        String responsePayload = response.getPayloadAsString();
        assertThat(responsePayload, Matchers.containsString(expectedErrorMessage));
    }

}
