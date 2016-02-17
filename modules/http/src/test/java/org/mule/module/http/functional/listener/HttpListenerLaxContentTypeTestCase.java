/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.http.functional.listener;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.mule.api.config.MuleProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.module.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.module.http.api.HttpHeaders.Names.CONTENT_TYPE;

import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.api.config.MuleProperties;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.util.IOUtils;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.fluent.Request;
import org.junit.Rule;
import org.junit.Test;

public class HttpListenerLaxContentTypeTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort httpPort = new DynamicPort("httpPort");

    @Rule
    public SystemProperty laxContentType = new SystemProperty(SYSTEM_PROPERTY_PREFIX + "laxContentType", Boolean.TRUE.toString());

    @Override
    protected String getConfigFile()
    {
        return "http-listener-lax-content-type-config.xml";
    }

    @Test
    public void ignoresInvalidContentTypeWithoutBody() throws Exception
    {
        Request request = Request.Post(getUrl()).addHeader(CONTENT_TYPE, "application");
        testIgnoreInvalidContentType(request, "{ \"key1\" : \"value, \"key2\" : 2 }");
    }

    @Test
    public void returnsInvalidContentTypeInResponse() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        MuleMessage response = client.send(getUrlForIvalid(), TEST_MESSAGE, null);

        assertInvalidContentTypeProperty(response);
    }

    private void testIgnoreInvalidContentType(Request request, String expectedMessage) throws IOException
    {
        HttpResponse response = request.execute().returnResponse();
        StatusLine statusLine = response.getStatusLine();

        assertThat(IOUtils.toString(response.getEntity().getContent()), containsString(expectedMessage));
        assertThat(statusLine.getStatusCode(), is(OK.getStatusCode()));
    }

    private String getUrl()
    {
        return String.format("http://localhost:%s/testInput", httpPort.getValue());
    }

    private String getUrlForIvalid()
    {
        return String.format("http://localhost:%s/testInputInvalid", httpPort.getValue());
    }

    private void assertInvalidContentTypeProperty(MuleMessage response)
    {
        assertThat(response.getInboundPropertyNames(), hasItem(equalToIgnoringCase(MuleProperties.CONTENT_TYPE_PROPERTY)));
        assertThat((String) response.getInboundProperty(MuleProperties.CONTENT_TYPE_PROPERTY), equalTo("invalidMimeType"));
    }
}
