/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.http.functional.requester;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.hasItem;
import static org.mule.api.config.MuleProperties.CONTENT_TYPE_PROPERTY;

import org.mule.api.MuleMessage;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.junit.Rule;
import org.junit.Test;

public class HttpRequestBuilderContentTypeTestCase extends AbstractHttpRequestTestCase
{

    @Rule
    public DynamicPort httpListenerPort = new DynamicPort("httpListenerPort");

    @Override
    protected String getConfigFile()
    {
        return "http-request-builder-content-type-config.xml";
    }

    /**
     * Content-Type set in request builder is not considered.
     * See MULE-9566
     */
    @Test
    public void contentTypeInRequestBuilderSentAsHeader() throws Exception
    {
        MuleMessage response = runFlow("requesterContentTypeClient").getMessage();

        assertThat(response.getInboundPropertyNames(), hasItem(equalToIgnoringCase(CONTENT_TYPE_PROPERTY)));
        assertThat((String) response.getInboundProperty(CONTENT_TYPE_PROPERTY), equalTo("text/x-json"));
    }

    @Override
    protected void handleRequest(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        response.setContentType(request.getContentType());
    }

}
