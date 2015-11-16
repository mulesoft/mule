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
import static org.mule.module.http.api.HttpConstants.HttpStatus.BAD_REQUEST;
import static org.mule.module.http.api.HttpHeaders.Names.CONTENT_TYPE;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.api.config.MuleProperties;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.util.IOUtils;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.StringEntity;
import org.junit.Rule;
import org.junit.Test;

public class HttpListenerContentTypeTestCase extends FunctionalTestCase
{

    private static final String EXPECTED_CONTENT_TYPE = "application/json; charset=UTF-8";

    @Rule
    public DynamicPort httpPort = new DynamicPort("httpPort");

    @Override
    protected String getConfigFile()
    {
        return "http-listener-content-type-config.xml";
    }

    @Test
    public void returnsContentTypeInResponse() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        MuleMessage response = client.send(getUrl(), TEST_MESSAGE, null);

        assertContentTypeProperty(response);
    }

    @Test
    public void rejectsInvalidContentTypeWithoutBody() throws Exception
    {
        Request request = Request.Post(getUrl()).addHeader(CONTENT_TYPE, "application");
        testRejectContentType(request, "Invalid Content-Type");
    }

    @Test
    public void rejectsInvalidContentTypeWithBody() throws Exception
    {
        Request request = Request.Post(getUrl()).body(new StringEntity(TEST_MESSAGE, "application", null));
        testRejectContentType(request, "Could not parse");
    }

    private void testRejectContentType(Request request, String expectedMessage) throws IOException
    {
        HttpResponse response = request.execute().returnResponse();
        StatusLine statusLine = response.getStatusLine();

        assertThat(IOUtils.toString(response.getEntity().getContent()), containsString(expectedMessage));
        assertThat(statusLine.getStatusCode(), is(BAD_REQUEST.getStatusCode()));
        assertThat(statusLine.getReasonPhrase(), is(BAD_REQUEST.getReasonPhrase()));
    }

    private String getUrl()
    {
        return String.format("http://localhost:%s/testInput", httpPort.getValue());
    }

    private void assertContentTypeProperty(MuleMessage response)
    {
        assertThat(response.getInboundPropertyNames(), hasItem(equalToIgnoringCase(MuleProperties.CONTENT_TYPE_PROPERTY)));
        assertThat((String) response.getInboundProperty(MuleProperties.CONTENT_TYPE_PROPERTY), equalTo(EXPECTED_CONTENT_TYPE));
    }
}
