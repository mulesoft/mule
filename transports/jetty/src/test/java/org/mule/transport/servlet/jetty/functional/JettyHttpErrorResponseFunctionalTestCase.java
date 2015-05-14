/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.servlet.jetty.functional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import org.mule.api.config.MuleProperties;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConstants;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Rule;
import org.junit.Test;

public class JettyHttpErrorResponseFunctionalTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
    {
        return "jetty-http-encoding-test-flow.xml";
    }

    @Test
    public void sendsErrorResponseWhenFailsCreatingMessageOnReceiver() throws Exception
    {
        GetMethod request = new GetMethod("http://localhost:" + dynamicPort.getValue() + "/");
        request.addRequestHeader(MuleProperties.CONTENT_TYPE_PROPERTY, "invalidMimeType");
        HttpClient httpClient = new HttpClient();

        int responseCode = httpClient.executeMethod(request);

        assertThat(responseCode, equalTo(HttpConstants.SC_INTERNAL_SERVER_ERROR));
    }
}
