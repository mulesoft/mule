/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.issues;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.fluent.Response;
import org.junit.Test;
import org.mule.tck.junit4.FunctionalTestCase;

import static org.apache.http.HttpVersion.HTTP_1_0;
import static org.apache.http.client.fluent.Request.Get;
import static org.apache.http.client.fluent.Request.Post;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpHeaders.Names.TRANSFER_ENCODING;

public class Mule18681TestCase extends FunctionalTestCase {

    private static final int DEFAULT_TIMEOUT = 1000;

    protected HttpVersion getHttpVersion()
    {
        return HTTP_1_0;
    }

    protected String getUrl()
    {
        return "http://localhost:8081/test";
    }

    @Override
    protected String getConfigFile()
    {
        return "http-10-chunked.xml";
    }

    @Test
    public void httpOutput() throws Exception
    {
        // MULE-18493: HTTP Connector leaking sockets in state CLOSE_WAIT when client uses HTTP/1.0 due to a NPE.
        final String url = getUrl();

        final Response response = Get(url).version(getHttpVersion())
                .connectTimeout(DEFAULT_TIMEOUT)
                .socketTimeout(DEFAULT_TIMEOUT)
                .execute();
        final HttpResponse httpResponse = response.returnResponse();
        final Header transferEncodingHeader = httpResponse.getFirstHeader(TRANSFER_ENCODING);
        assertThat(transferEncodingHeader, is(nullValue()));
    }

}
