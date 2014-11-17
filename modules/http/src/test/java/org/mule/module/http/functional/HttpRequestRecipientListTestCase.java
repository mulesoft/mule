/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleMessageCollection;
import org.mule.api.transport.PropertyScope;
import org.mule.construct.Flow;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Arrays;

import org.hamcrest.core.IsInstanceOf;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

@Ignore
public class HttpRequestRecipientListTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort port1 = new DynamicPort("port1");
    @Rule
    public DynamicPort port2 = new DynamicPort("port2");
    @Rule
    public DynamicPort port3 = new DynamicPort("port3");

    @Override
    protected String getConfigFile()
    {
        return "http-request-recipient-list-config.xml";
    }

    @Test
    public void recipientListWithHttpUrlsWithResponse() throws Exception
    {
        final MuleEvent testEvent = getTestEvent(TEST_MESSAGE);
        testEvent.getMessage().setProperty("urls", Arrays.asList(getUrlForPort(port1), getUrlForPort(port2), getUrlForPort(port3)), PropertyScope.INBOUND);
        final MuleEvent response = ((Flow) getFlowConstruct("recipientListFlow")).process(testEvent);
        assertThat(response, notNullValue());
        assertThat(response.getMessage(), IsInstanceOf.instanceOf(MuleMessageCollection.class));
        MuleMessageCollection aggregatedResponse = (MuleMessageCollection) response.getMessage();
        assertThat(aggregatedResponse.size(), is(3));
        final MuleMessage[] messages = aggregatedResponse.getMessagesAsArray();
        for (int i = 0; i < messages.length; i++)
        {
            MuleMessage message = messages[i];
            assertThat(message, notNullValue());
            assertThat(message.getPayloadAsString(), is("inXFlowResponse".replace("X", String.valueOf(i + 1))));
        }
    }

    private String getUrlForPort(DynamicPort port)
    {
        return String.format("http://localhost:%s/path", port.getNumber());
    }
}
