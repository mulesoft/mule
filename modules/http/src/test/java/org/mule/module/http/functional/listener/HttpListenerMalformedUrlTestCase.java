/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.listener;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;
import static org.mule.module.http.api.HttpConstants.HttpStatus.BAD_REQUEST;
import static org.mule.module.http.api.HttpConstants.HttpStatus.OK;

import org.mule.module.http.utils.SocketRequester;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import com.google.common.base.Charsets;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.http.client.fluent.Request;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.junit.Rule;
import org.junit.Test;
import org.mule.util.IOUtils;

public class HttpListenerMalformedUrlTestCase extends FunctionalTestCase
{

    public static final String TEST_MALFORMED = "api/ping%";
    public static final String TEST_MALFORMED_SCRIPT = "<script></script>%";
    public static final String TEST_ENCODED_SPACE = "test/foo 1 %";
    public static final String TEST_ENCODED_HASHTAG = "test/foo 1 #";
    public static final String TEST_ENCODED_PERCENT2 = "test/%24";

    @Rule
    public DynamicPort httpPort = new DynamicPort("httpPort");

    @Override
    protected String getConfigFile()
    {
        return "http-listener-malformed-url-config.xml";
    }

    @Test
    public void returnsBadRequestOnMalformedUrlForWildcardEndpoint() throws Exception
    {
        SocketRequester socketRequester = new SocketRequester("localhost", httpPort.getNumber());;
        try
        {
            socketRequester.initialize();
            socketRequester.doRequest("GET /" + TEST_MALFORMED + " HTTP/1.1");
            String response = socketRequester.getResponse();
            assertThat(response, containsString(Integer.toString(BAD_REQUEST.getStatusCode())));
            assertThat(response, containsString(BAD_REQUEST.getReasonPhrase()));
            assertThat(response, containsString("Unable to parse request: /"+TEST_MALFORMED));
        }
        finally
        {
            socketRequester.finalizeGracefully();
        }
    }

    @Test
    public void returnsBadRequestOnMalformedUrlWithInvalidContentTypeWithScript() throws Exception
    {
        SocketRequester socketRequester = new SocketRequester("localhost", httpPort.getNumber());;
        try
        {
            socketRequester.initialize();
            socketRequester.doRequest("POST /"+TEST_MALFORMED_SCRIPT+" HTTP/1.1");
            String response = socketRequester.getResponse();
            assertThat(response, containsString(Integer.toString(BAD_REQUEST.getStatusCode())));
            assertThat(response, containsString(BAD_REQUEST.getReasonPhrase()));
            assertThat(response, containsString(escapeHtml(TEST_MALFORMED_SCRIPT)));
        }
        finally
        {
            socketRequester.finalizeGracefully();
        }
    }

    @Test
    public void returnsOKWithEndocodedPathForSpecificEndpointSpace() throws Exception
    {
        assertPostRequestGetsOKResponseStatusAndPayload(TEST_ENCODED_SPACE, "specific");
    }

    @Test
    public void returnsOKWithEndocodedPathForSpecificEndpointHashtag() throws Exception
    {
        assertPostRequestGetsOKResponseStatusAndPayload(TEST_ENCODED_HASHTAG, "specific2");
    }

    @Test
    public void returnsOKWithEndocodedPathForSpecificEndpointPercent() throws Exception
    {
        assertPostRequestGetsOKResponseStatusAndPayload(TEST_ENCODED_PERCENT2, "specific3");
    }

    public void assertPostRequestGetsOKResponseStatusAndPayload(String endpoint, String payload) throws Exception
    {
        Request request = Request.Post(getUrl(endpoint));

        HttpResponse response = request.execute().returnResponse();
        StatusLine statusLine = response.getStatusLine();

        assertThat(statusLine.getStatusCode(), is(OK.getStatusCode()));
        assertThat(IOUtils.toString(response.getEntity().getContent()), is(payload));
    }

    protected String getUrl(String path) throws UnsupportedEncodingException
    {
        return format("http://localhost:%s/%s", httpPort.getValue(), URLEncoder.encode(path, Charsets.UTF_8.displayName()));
    }

}
