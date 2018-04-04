/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.issues;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpConstants.HttpStatus.ACCEPTED;
import static org.mule.module.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.module.http.api.HttpConstants.Methods.POST;
import static org.mule.module.http.api.HttpConstants.ResponseProperties.HTTP_STATUS_PROPERTY;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;

import org.junit.Rule;
import org.junit.Test;
import org.mule.api.MuleMessage;
import org.mule.module.http.api.client.HttpRequestOptions;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

public class ProxyServiceWithMultipleOperationsSameBinding extends FunctionalTestCase
{

    private static final String SOAP_REQUEST_REQUEST_RESPONSE =
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                                               "<soap:Body><echo2 xmlns=\"http://www.muleumo.org\"> foo </echo2></soap:Body>" +
                                               "</soap:Envelope>";
    
    private static final String SOAP_REQUEST_ONE_WAY =
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                                               "<soap:Body><log2 xmlns=\"http://www.muleumo.org\"> foo </log2></soap:Body>" +
                                               "</soap:Envelope>";
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port");

    private static final HttpRequestOptions HTTP_REQUEST_OPTIONS = newOptions().method(POST.name()).disableStatusCodeValidation().build();

    
    @Test
    public void testStatusCodeOperationOneWay() throws Exception
    {
        MuleMessage msg = getTestMuleMessage(SOAP_REQUEST_ONE_WAY);
        MuleMessage muleMessage = muleContext.getClient().send("http://localhost:" + dynamicPort.getNumber() + "/test", msg, HTTP_REQUEST_OPTIONS);
        assertThat(muleMessage.<Integer> getInboundProperty(HTTP_STATUS_PROPERTY), is(ACCEPTED.getStatusCode()));
    }
    
    @Test
    public void testStatusCodeOperationRequestResponse() throws Exception
    {
        MuleMessage msg = getTestMuleMessage(SOAP_REQUEST_REQUEST_RESPONSE);
        MuleMessage muleMessage = muleContext.getClient().send("http://localhost:" + dynamicPort.getNumber() + "/test", msg, HTTP_REQUEST_OPTIONS);
        assertThat(muleMessage.<Integer> getInboundProperty(HTTP_STATUS_PROPERTY), is(OK.getStatusCode()));
    }

    @Override
    protected String getConfigFile()
    {
        return "issues/proxy-service-multiple-operations-same-binding.xml";
    }

}
