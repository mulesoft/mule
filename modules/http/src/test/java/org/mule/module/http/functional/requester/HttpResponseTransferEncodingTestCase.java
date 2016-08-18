/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;


import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleEvent;
import org.mule.construct.Flow;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.tcp.SimpleServerSocket;
import org.mule.util.IOUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class HttpResponseTransferEncodingTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort port = new DynamicPort("httpPort");

    private SimpleServerSocket simpleServerSocket;

    @Override
    protected String getConfigFile()
    {
        return "http-response-transfer-encoding-config.xml";
    }

    @Before
    public void setup() throws Exception
    {
        simpleServerSocket = new SimpleServerSocket(port.getNumber(), IOUtils.getResourceAsString
                ("http-response-transfer-encoding-response.txt", getClass()));
        newSingleThreadExecutor().submit(simpleServerSocket);
    }

    @After
    public void tearDown()
    {
        simpleServerSocket.close();
    }

    @Test
    public void chunkedTakesPrecedenceOverContentLength() throws Exception
    {
        MuleEvent response = ((Flow) getFlowConstruct("responseStreaming")).process(getTestEvent(TEST_MESSAGE));
        assertThat(response.getMessage().getPayloadAsString(), equalTo(TEST_MESSAGE));
    }

}
