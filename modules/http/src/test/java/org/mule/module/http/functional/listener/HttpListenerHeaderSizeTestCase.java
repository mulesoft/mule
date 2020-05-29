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
import static org.mule.module.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.module.http.api.HttpConstants.HttpStatus.REQUEST_TOO_LONG;
import static org.mule.module.http.internal.listener.grizzly.GrizzlyServerManager.MAXIMUM_HEADER_SECTION_SIZE_PROPERTY_KEY;

import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

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
        int queryParamSize = Integer.parseInt(maxHeaderSectionSizeSystemProperty.getValue()) + SIZE_DELTA;
        Response response = sendRequestWithQueryParam(queryParamSize);
        assertThat(response.returnResponse().getStatusLine().getStatusCode(), is(REQUEST_TOO_LONG.getStatusCode()));
    }

    @Test
    public void maxHeaderSizeNotExceeded() throws Exception
    {
        int queryParamSize = Integer.parseInt(maxHeaderSectionSizeSystemProperty.getValue()) - SIZE_DELTA;
        Response response = sendRequestWithQueryParam(queryParamSize);
        assertThat(response.returnResponse().getStatusLine().getStatusCode(), is(OK.getStatusCode()));

    }

    private Response sendRequestWithQueryParam(int queryParamSize) throws IOException
    {
        String longHeaderValue = RandomStringUtils.randomAlphanumeric(queryParamSize);
        String urlWithQueryParameter = format("http://localhost:%d/", dynamicPort.getNumber());
        return Request.Get(urlWithQueryParameter).setHeader("header", longHeaderValue)
            .execute();
    }

    @Override
    protected String getConfigFile()
    {
        return "http-listener-max-header-size-config.xml";
    }
}
