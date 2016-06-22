/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.listener;

import static java.lang.Boolean.TRUE;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.functional.HttpHeaderCaseTestCase.HEADER_NAME;
import static org.mule.module.http.functional.HttpHeaderCaseTestCase.PRESERVE_HEADER_CASE;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.util.IOUtils;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.junit.Rule;
import org.junit.Test;

public class HttpListenerHeaderCaseTestCase extends FunctionalTestCase
{
    public static final String HEADER_VALUE = "customValue";

    @Rule
    public DynamicPort port = new DynamicPort("port");
    @Rule
    public SystemProperty headerCaseProperty = new SystemProperty(PRESERVE_HEADER_CASE, TRUE.toString());

    @Override
    protected String getConfigFile()
    {
        return "http-listener-header-case-config.xml";
    }

    @Test
    public void sendsAndReceivesHeaderWithSameCase() throws Exception
    {
        final String url = String.format("http://localhost:%s/test", port.getNumber());
        final Response response = Request.Get(url).setHeader(HEADER_NAME, HEADER_VALUE).connectTimeout(RECEIVE_TIMEOUT).execute();
        HttpResponse httpResponse = response.returnResponse();

        assertThat(IOUtils.toString(httpResponse.getEntity().getContent()), is(TRUE.toString()));
        assertThat(httpResponse.getFirstHeader(HEADER_NAME).getValue(), is(HEADER_VALUE));
    }

}
