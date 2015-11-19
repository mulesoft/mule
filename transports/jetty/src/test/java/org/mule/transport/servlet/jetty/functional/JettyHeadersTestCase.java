/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet.jetty.functional;

import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpConstants.HttpStatus.OK;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Rule;
import org.junit.Test;

public class JettyHeadersTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort httpPort = new DynamicPort("httpPort");

    @Override
    protected String getConfigFile()
    {
        return "jetty-headers-config.xml";
    }

    @Test
    public void handlesEmptyHeader() throws Exception
    {
        String url = String.format("http://localhost:%s/testHeaders", httpPort.getNumber());
        GetMethod method = new GetMethod(url);
        method.addRequestHeader("Accept", null);
        int statusCode = new HttpClient().executeMethod(method);

        assertThat(statusCode, is(OK.getStatusCode()));
        assertThat(method.getResponseBodyAsString(), is(EMPTY));
    }
}
