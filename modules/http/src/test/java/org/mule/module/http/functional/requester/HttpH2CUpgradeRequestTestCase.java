/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.nio.charset.Charset;

import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.junit.Rule;
import org.junit.Test;
import org.mule.api.MuleMessage;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

public class HttpH2CUpgradeRequestTestCase extends FunctionalTestCase
{

    private static final String TEST_MESSAGE = "test";

    private static final String URL = "http://localhost:%s/test";

    @Rule
    public DynamicPort listenPort = new DynamicPort("port");

    @Override
    protected String getConfigFile()
    {
        return "http-listener-h2c-request-config.xml";
    }

    /**
     * Due to the fact that grizzly does not apply the HTTP/2 filters in the version mule is based on, mule grizzly
     * filter handles the Incoming and Outgoing Upgrade events so that the HTTP request is not hang.
     */
    @Test
    public void requestWithH2CUpgradeHeader() throws Exception
    {
        final String url = String.format(URL, listenPort.getNumber());
        Request request = Request.Post(url).bodyString(TEST_MESSAGE, ContentType.create("text/plain", Charset.forName("UTF-8"))).addHeader("Upgrade", "h2c");

        request.execute();
        MuleMessage result = muleContext.getClient().request("vm://out", 2000);
        assertThat(result.getPayloadAsString(), is(TEST_MESSAGE));
    }
}
