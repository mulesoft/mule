/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static java.lang.Boolean.TRUE;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.functional.HttpHeaderCaseTestCase.HEADER_NAME;
import static org.mule.module.http.functional.HttpHeaderCaseTestCase.PRESERVE_HEADER_CASE;
import org.mule.api.MuleMessage;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.junit.Rule;
import org.junit.Test;

public class HttpRequestHeaderCaseTestCase extends AbstractHttpRequestTestCase
{
    @Rule
    public SystemProperty headerCaseProperty = new SystemProperty(PRESERVE_HEADER_CASE, TRUE.toString());

    @Override
    protected String getConfigFile()
    {
        return "http-request-header-case-config.xml";
    }

    @Override
    protected void handleRequest(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        super.handleRequest(baseRequest, request, response);
        String headerValue = request.getHeader(HEADER_NAME);
        response.addHeader(HEADER_NAME, headerValue);
    }

    @Test
    public void sendsAndReceivesHeaderWithSameCase() throws Exception
    {
        MuleMessage response = runFlow("client").getMessage();
        assertThat(response.getPayloadAsString(), is(TRUE.toString()));
    }

}
