/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

public class HttpRequestResponseHeadersTestCase extends AbstractHttpRequestTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "http-request-response-headers-config.xml";
    }

    /**
     * This case is not valid according to the RFC (https://http2.github.io/http2-spec/),
     * but in any case, it shouldn't cause the request to fail. 
     */
    @Test
    public void responseWithUpgradeToHttp2Header() throws Exception
    {
        assertThat(runFlow("responseWithUpgradeToHttp2Header", getTestEvent(TEST_MESSAGE)), not(nullValue()));
    }

    @Override
    protected void writeResponse(HttpServletResponse response) throws IOException
    {
        response.addHeader("Upgrade", "h2,h2c");
        response.addHeader("Conection", "Upgrade, close");
        super.writeResponse(response);
    }
}


