/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet.jetty.functional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mule.api.config.MuleProperties.CONTENT_TYPE_PROPERTY;
import static org.mule.transport.http.HttpConstants.SC_INTERNAL_SERVER_ERROR;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;

public class JettyHttpBadEncodingFunctionalTestCase extends JettyHttpEncodingFunctionalTestCase
{
    public JettyHttpBadEncodingFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Override
    public void testSend() throws Exception
    {
        GetMethod request = new GetMethod("http://localhost:" + dynamicPort.getValue());
        request.addRequestHeader(CONTENT_TYPE_PROPERTY, "text/bar; charset=UTFF-912");
        HttpClient httpClient = new HttpClient();

        int responseCode = httpClient.executeMethod(request);

        assertThat(responseCode, equalTo(SC_INTERNAL_SERVER_ERROR));
    }
}
