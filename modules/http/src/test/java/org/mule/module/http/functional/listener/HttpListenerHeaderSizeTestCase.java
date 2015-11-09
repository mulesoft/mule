/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.listener;

import static java.lang.String.format;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpConstants.HttpStatus.BAD_REQUEST;
import static org.mule.module.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.module.http.api.HttpConstants.ResponseProperties.HTTP_REASON_PROPERTY;
import static org.mule.module.http.api.HttpConstants.ResponseProperties.HTTP_STATUS_PROPERTY;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import static org.mule.module.http.internal.HttpParser.appendQueryParam;
import static org.mule.module.http.internal.listener.grizzly.GrizzlyServerManager.MAXIMUM_HEADER_SECTION_SIZE_PROPERTY_KEY;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Rule;
import org.junit.Test;

public class HttpListenerHeaderSizeTestCase extends FunctionalTestCase
{

    private static final int SIZE_DELTA = 1000;

    @Rule
    public SystemProperty maxHeaderSectionSizeSystemProperty = new SystemProperty(MAXIMUM_HEADER_SECTION_SIZE_PROPERTY_KEY, "10000");
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port");

    @Test
    public void maxHeaderSizeExceeded() throws Exception
    {
        MuleMessage response = sendRequestWithQueryParam(Integer.valueOf(maxHeaderSectionSizeSystemProperty.getValue()) + SIZE_DELTA);
        assertThat(response.<Integer>getInboundProperty(HTTP_STATUS_PROPERTY), is(BAD_REQUEST.getStatusCode()));
        assertThat(response.<String>getInboundProperty(HTTP_REASON_PROPERTY), is(BAD_REQUEST.getReasonPhrase()));
    }

    @Test
    public void maxHeaderSizeNotExceeded() throws Exception
    {
        int queryParamSize = Integer.valueOf(maxHeaderSectionSizeSystemProperty.getValue()) - SIZE_DELTA;
        MuleMessage response = sendRequestWithQueryParam(queryParamSize);
        assertThat(response.<Integer>getInboundProperty(HTTP_STATUS_PROPERTY), is(OK.getStatusCode()));
        assertThat(response.getPayloadAsBytes().length, is(queryParamSize));
    }

    private MuleMessage sendRequestWithQueryParam(int queryParamSize) throws MuleException
    {
        String longQueryParamValue = RandomStringUtils.randomAlphanumeric(queryParamSize);
        String urlWithQueryParameter = appendQueryParam(format("http://localhost:%d/", dynamicPort.getNumber()), "longQueryParam", longQueryParamValue);
        return muleContext.getClient().send(urlWithQueryParameter, getTestMuleMessage(), newOptions().disableStatusCodeValidation().build());
    }

    @Override
    protected String getConfigFile()
    {
        return "http-listener-max-header-size-config.xml";
    }
}
