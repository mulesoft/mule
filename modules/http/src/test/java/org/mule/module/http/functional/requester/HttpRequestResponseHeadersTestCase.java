/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpHeaders.Values.CHUNKED;

import org.mule.api.MuleEvent;
import org.mule.construct.Flow;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.Rule;
import org.junit.Test;

public class HttpRequestResponseHeadersTestCase extends AbstractHttpRequestTestCase
{

    @Rule
    public SystemProperty host = new SystemProperty("host", "localhost");
    @Rule
    public SystemProperty encoding = new SystemProperty("encoding" , CHUNKED);

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
    public void responseWithUpgradeToHttp2Header() throws Exception {
        Flow flow = (Flow) getFlowConstruct("responseWithUpgradeToHttp2Header");

        MuleEvent event = getTestEvent(TEST_MESSAGE);
        flow.process(event);
    }

    @Override
    protected void writeResponse(HttpServletResponse response) throws IOException
    {
        response.addHeader("Upgrade", "h2,h2c");
//        response.addHeader("Conection", "Upgrade, close");
        super.writeResponse(response);
    }
}


