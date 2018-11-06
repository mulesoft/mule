/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.listener;

import static java.lang.String.format;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.module.http.api.HttpConstants.HttpStatus;
import static org.mule.module.http.api.HttpConstants.HttpStatus.BAD_REQUEST;
import static org.mule.module.http.api.HttpConstants.HttpStatus.OK;

import org.mule.util.IOUtils;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.fluent.Request;

public class HttpListenerIncompleteMultipartRequestTestCase extends FunctionalTestCase
{
    private static final String END_LINE = System.lineSeparator();
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String CONTENT_TYPE_MULTIPART = "multipart/form-data";
    private static final String PART_HEADER = "Content-Disposition: form-data; name=\"field2\"";
    private static final String BOUNDARY_DEF = "boundary=\"BOUNDARY\"";
    private static final String BOUNDARY_START = "--BOUNDARY";
    private static final String BOUNDARY_END = "--BOUNDARY--";
    private static final String TEST_EMAIL = "api/email";

    @Rule
    public DynamicPort httpPort = new DynamicPort("httpPort");

    @Override
    protected String getConfigFile()
    {
        return "http-listener-incomplete-multipart-config.xml";
    }

    @Test
    public void returnsOKOnMultipartFormDataWithBoundaryStartAndEnd() throws Exception
    {
        String payload = "value1";
        String body = BOUNDARY_START + END_LINE + PART_HEADER + END_LINE + END_LINE + payload + END_LINE + BOUNDARY_END;
        sendPostByteArrayRequestAndValidateStatusCode(body, OK, "", payload);
    }

    @Test
    public void returnsBadRequestOnMultipartFormDataWithNoBoundariesAndNoValue() throws Exception
    {
        String body = "emptyContent=";
        sendPostByteArrayRequestAndValidateStatusCode(body, BAD_REQUEST, BAD_REQUEST.getReasonPhrase(),"HTTP request parsing failed with error: \"Unable to parse multipart payload\"");
    }

    protected String getUrl(String path)
    {
        return format("http://localhost:%s/%s", httpPort.getValue(),path);
    }

    protected void sendPostByteArrayRequestAndValidateStatusCode(String body, HttpStatus status, String msg, String payload) throws IOException
    {
        Request request = Request.Post(getUrl(TEST_EMAIL));
        request.addHeader(CONTENT_TYPE_HEADER, CONTENT_TYPE_MULTIPART + ";" + BOUNDARY_DEF);
        request.bodyByteArray(body.getBytes());

        HttpResponse response = request.execute().returnResponse();
        StatusLine statusLine = response.getStatusLine();

        assertThat(statusLine.getStatusCode(), is(status.getStatusCode()));
        assertThat(statusLine.getReasonPhrase(), containsString(msg));
        assertThat(IOUtils.toString(response.getEntity().getContent()), containsString(payload));
    }

}
