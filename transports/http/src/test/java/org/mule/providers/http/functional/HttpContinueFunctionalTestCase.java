/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.http.functional;

import java.net.URI;

import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.lang.time.StopWatch;

public class HttpContinueFunctionalTestCase extends HttpFunctionalTestCase
{

    /**
     * HttpClient has default 3 seconds wait for Expect-Continue calls.
     */
    public static final int DEFAULT_HTTP_CLIENT_CONTINUE_WAIT = 3000;

    protected StopWatch stopWatch;

    protected void sendTestData(int iterations) throws Exception
    {
        URI uri = getInDest().getUri();
        HttpClientParams params = new HttpClientParams();
        params.setVersion(HttpVersion.HTTP_1_1);
        params.setBooleanParameter(HttpClientParams.USE_EXPECT_CONTINUE, true);
        postMethod = new PostMethod(uri.toString());
        postMethod.setParams(params);
        postMethod.setRequestEntity(new StringRequestEntity(TEST_MESSAGE, TEST_CONTENT_TYPE, TEST_CHARSET));
        cnn = new HttpConnection(uri.getHost(), uri.getPort(), Protocol.getProtocol(uri.getScheme()));
        cnn.open();
        stopWatch = new StopWatch();
        stopWatch.start();
        postMethod.execute(new HttpState(), cnn);
    }

    protected void receiveAndTestResults() throws Exception
    {
        stopWatch.stop();
        String msg = postMethod.getResponseBodyAsString();
        assertNotNull(msg);
        assertEquals(TEST_MESSAGE + " Received", msg);
        long processingTime = stopWatch.getTime();
        if (processingTime > DEFAULT_HTTP_CLIENT_CONTINUE_WAIT)
        {
            fail("Server did not handle Expect=100-continue header properly,");
        }
    }
}
