/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.integration;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.PropertyScope;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.config.MuleProperties;

import java.util.HashMap;
import java.util.Map;

import org.hamcrest.core.Is;
import org.hamcrest.core.IsNull;
import org.junit.Test;

public class JmsResponseElementTestCase extends FunctionalTestCase
{
    public static final String MESSAGE = "A Message";
    public static final String EXPECTED_MODIFIED_MESSAGE = "A Message jms flow content";
    public static final int TIMEOUT = 3000;
    public static final int TINY_TIMEOUT = 300;

    @Override
    protected String getConfigFile()
    {
        return "integration/jms-response-element-config-flow.xml";
    }

    @Test
    public void testOutboundEndpointResponse() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://vminbound", "some message", null);
        assertThat(getPayloadAsString(response), is(EXPECTED_MODIFIED_MESSAGE));
        assertThat(response.<String>getProperty("test", PropertyScope.INBOUND), Is.is("test"));
        assertThat(response.getExceptionPayload(), IsNull.<Object>nullValue());
    }

    @Test
    public void testInboundEndpointResponse() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://vminbound2", MESSAGE, null);
        assertThat(getPayloadAsString(response), is(EXPECTED_MODIFIED_MESSAGE));
        assertThat(response.getExceptionPayload(), IsNull.<Object>nullValue());
    }

    @Test
    public void testInboundEndpointResponseWithReplyTo() throws Exception
    {
        MuleClient client = muleContext.getClient();

        Map<String, Object> messageProperties = new HashMap<String, Object>();
        String replyToUri = "jms://out2";
        messageProperties.put(MuleProperties.MULE_REPLY_TO_PROPERTY, replyToUri);

        client.dispatch("jms://out", MESSAGE, messageProperties);

        MuleMessage response = client.request(replyToUri, TIMEOUT);
        assertThat(getPayloadAsString(response), is(EXPECTED_MODIFIED_MESSAGE));
        assertThat(response.getExceptionPayload(), IsNull.<Object>nullValue());
        response = client.request(replyToUri, TINY_TIMEOUT);
        assertThat(response, IsNull.<Object>nullValue());
    }

    @Test
    public void testInboundEndpointOneWay() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("jms://in3", MESSAGE, null);
        assertThat(getPayloadAsString(response), is(EXPECTED_MODIFIED_MESSAGE));
        assertThat(response.getExceptionPayload(), IsNull.<Object>nullValue());
    }
}
