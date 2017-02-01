/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.functional;

import static java.lang.String.format;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpConstants.HttpStatus.BAD_REQUEST;
import static org.mule.module.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.module.http.api.HttpConstants.ResponseProperties.HTTP_STATUS_PROPERTY;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;

import org.mule.api.MuleMessage;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class HttpUriEncodingErrorTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port");

    @Test
    public void failsWithAppropriateError() throws Exception
    {
        String address = getUri() + "?blah=badcode%2";
        MuleMessage response = muleContext.getClient().send(address, getTestMuleMessage(), newOptions().disableStatusCodeValidation().build());

        assertThat(response.<Integer>getInboundProperty(HTTP_STATUS_PROPERTY), is(BAD_REQUEST.getStatusCode()));
        assertThat(response.getPayloadAsString(), containsString("URLDecoder"));
    }

    @Test
    public void worksWhenValidUri() throws Exception
    {
        String address = getUri() + "?blah=a%20space";
        MuleMessage response = muleContext.getClient().send(address, getTestMuleMessage());

        assertThat(response.<Integer>getInboundProperty(HTTP_STATUS_PROPERTY), is(OK.getStatusCode()));
        assertThat(response.getPayloadAsString(), equalTo("response"));
    }

    @Override
    protected String getConfigFile()
    {
        return "http-uri-encoding-error-config.xml";
    }

    private String getUri()
    {
        return format("http://localhost:%d/", dynamicPort.getNumber());
    }
}
