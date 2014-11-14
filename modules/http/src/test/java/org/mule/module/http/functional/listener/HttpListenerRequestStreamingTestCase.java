/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.listener;

import static org.junit.Assert.assertThat;

import org.mule.api.MuleMessage;
import org.mule.module.http.api.requester.HttpRequester;
import org.mule.module.http.api.requester.HttpRequesterBuilder;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.ByteArrayInputStream;

import org.hamcrest.core.Is;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

@Ignore //TODO fix - see MULE-8044
public class HttpListenerRequestStreamingTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort listenPort = new DynamicPort("port");

    @Override
    protected String getConfigFile()
    {
        return "http-listener-request-streaming-config.xml";
    }

    @Test
    public void listenerReceivedChunckedRequest() throws Exception
    {
        final HttpRequester requester = new HttpRequesterBuilder(muleContext)
                .setUrl(String.format("http://localhost:%s/", listenPort.getNumber()))
                .setMethod("POST").build();
        requester.process(getTestEvent(new ByteArrayInputStream(getPayload().getBytes())));
        final MuleMessage message = muleContext.getClient().request("vm://out", RECEIVE_TIMEOUT);
        assertThat(message.getPayloadAsString(), Is.is(getPayload()));
    }

    private String getPayload()
    {
        //generate 100kb payload as minimum
        int numberOfTestMessagesRequired = (1024*100)/TEST_MESSAGE.getBytes().length;
        final StringBuilder bigPayload = new StringBuilder();
        for (int i = 0; i < numberOfTestMessagesRequired; i++)
        {
            bigPayload.append(TEST_MESSAGE + "\n");
        }
        return bigPayload.toString();
    }

    @Override
    public int getTestTimeoutSecs()
    {
        return 9999999;
    }
}
