/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.functional.requester;

import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.module.http.functional.matcher.HttpMessageAttributesMatchers.hasStatusCode;
import org.mule.extension.http.api.HttpResponseAttributes;
import org.mule.runtime.core.api.MuleMessage;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.junit.Test;

public class HttpRequest401TestCase extends AbstractHttpRequestTestCase
{

    private static final String UNAUTHORIZED_MESSAGE = "Unauthorized: check credetials.";

    @Override
    protected void handleRequest(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        response.setStatus(SC_UNAUTHORIZED);
        response.getWriter().print(UNAUTHORIZED_MESSAGE);
    }

    @Override
    protected String getConfigFile()
    {
        return "http-request-401-config.xml";
    }

    @Test
    public void returns401Response() throws Exception
    {
        MuleMessage response = runFlow("executeRequest").getMessage();
        assertThat((HttpResponseAttributes) response.getAttributes(), hasStatusCode(SC_UNAUTHORIZED));
        assertThat(getPayloadAsString(response), is(UNAUTHORIZED_MESSAGE));
    }

}
