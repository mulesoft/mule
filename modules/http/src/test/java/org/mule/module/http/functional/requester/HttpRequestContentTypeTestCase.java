/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.http.functional.requester;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mule.api.config.MuleProperties.CONTENT_TYPE_PROPERTY;
import static org.mule.api.config.MuleProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.transformer.types.MimeTypes.HTML;
import org.mule.api.MuleMessage;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.junit.Rule;
import org.junit.Test;

public class HttpRequestContentTypeTestCase extends AbstractHttpRequestTestCase
{

    private static final String EXPECTED_CONTENT_TYPE = "application/json; charset=UTF-8";

    private String requestResponse = DEFAULT_RESPONSE;
    private String responseContentType = "text/html";

    @Rule
    public SystemProperty strictContentType = new SystemProperty(SYSTEM_PROPERTY_PREFIX + "strictContentType", Boolean.TRUE.toString());

    @Override
    protected String getConfigFile()
    {
        return "http-request-content-type-config.xml";
    }

    @Test
    public void sendsContentTypeFromPayload() throws Exception
    {
        validateContentTypeFromFlow("payload");
    }

    @Test
    public void sendsContentTypeFromHeader() throws Exception
    {
        validateContentTypeFromFlow("header");
    }

    @Test
    public void sendsContentTypeFromProperty() throws Exception
    {
        validateContentTypeFromFlow("property");
    }

    @Test
    public void returnsContentTypeWhenAvailable() throws Exception
    {
        MuleMessage result = runFlow("payload").getMessage();
        assertThat(result.getDataType().getMimeType(), is(HTML));
    }

    @Test
    public void returnsDefaultContentTypeWhenNotAvailable() throws Exception
    {
        responseContentType = null;
        MuleMessage result = runFlow("payload").getMessage();
        assertThat(result.getDataType().getMimeType(), is("application/octet-stream"));
    }

    @Test
    public void returnsNoContentTypeWhenResponseIsEmpty() throws Exception
    {
        requestResponse = null;
        responseContentType = null;
        MuleMessage result = runFlow("payload").getMessage();
        assertThat(result.getDataType().getMimeType(), is("*/*"));
    }

    @Override
    protected void writeResponse(HttpServletResponse response) throws IOException
    {
        response.setStatus(HttpServletResponse.SC_OK);
        if (requestResponse != null)
        {
            response.getWriter().print(requestResponse);
            if (responseContentType != null)
            {
                response.setContentType(responseContentType);
            }
        }
    }

    private void validateContentTypeFromFlow(String flowName) throws Exception
    {
        runFlow(flowName, TEST_MESSAGE);
        assertThat(getFirstReceivedHeader(CONTENT_TYPE_PROPERTY), equalTo(EXPECTED_CONTENT_TYPE));
    }

}
