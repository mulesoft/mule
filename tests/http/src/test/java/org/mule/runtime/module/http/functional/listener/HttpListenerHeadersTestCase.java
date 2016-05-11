/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.functional.listener;

import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.module.http.api.HttpConstants.HttpStatus.OK;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.module.http.functional.AbstractHttpTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.junit.Rule;
import org.junit.Test;

public class HttpListenerHeadersTestCase extends AbstractHttpTestCase
{
    @Rule
    public DynamicPort listenPort = new DynamicPort("port");

    @Override
    protected String getConfigFile()
    {
        return "http-listener-headers-config.xml";
    }

    @Test
    public void handlesEmptyHeader() throws Exception
    {
        String url = String.format("http://localhost:%s/testHeaders", listenPort.getNumber());
        HttpResponse response = Request.Post(url).addHeader("Accept", null).execute().returnResponse();

        assertThat(response.getStatusLine().getStatusCode(), is(OK.getStatusCode()));
        assertThat(IOUtils.toString(response.getEntity().getContent()), is(EMPTY));
    }


}
