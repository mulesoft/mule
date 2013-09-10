/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms.integration;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.config.MuleProperties;
import org.mule.api.transport.PropertyScope;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.hamcrest.core.Is;
import org.hamcrest.core.IsNull;
import org.junit.Test;
import org.junit.runners.Parameterized;

public class JmsResponseElementTestCase extends AbstractServiceAndFlowTestCase
{
    public static final String MESSAGE = "A Message";
    public static final String EXPECTED_MODIFIED_MESSAGE = "A Message jms flow content";
    public static final int TIMEOUT = 3000;
    public static final int TINY_TIMEOUT = 300;

    public JmsResponseElementTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
                {ConfigVariant.SERVICE, "integration/jms-response-element-config-service.xml"},
                {ConfigVariant.FLOW, "integration/jms-response-element-config-flow.xml"}
        });
    }

    @Test
    public void testOutboundEndpointResponse() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://vminbound", "some message", null);
        assertThat(response.getPayloadAsString(), is(EXPECTED_MODIFIED_MESSAGE));
        assertThat(response.<String>getProperty("test", PropertyScope.INBOUND), Is.is("test"));
        assertThat(response.getExceptionPayload(), IsNull.<Object>nullValue());
    }

    @Test
    public void testInboundEndpointResponse() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://vminbound2", MESSAGE, null);
        assertThat(response.getPayloadAsString(), is(EXPECTED_MODIFIED_MESSAGE));
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
        assertThat(response.getPayloadAsString(), is(EXPECTED_MODIFIED_MESSAGE));
        assertThat(response.getExceptionPayload(), IsNull.<Object>nullValue());
        response = client.request(replyToUri, TINY_TIMEOUT);
        assertThat(response, IsNull.<Object>nullValue());
    }

    @Test
    public void testInboundEndpointOneWay() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("jms://in3", MESSAGE, null);
        assertThat(response.getPayloadAsString(), is(EXPECTED_MODIFIED_MESSAGE));
        assertThat(response.getExceptionPayload(), IsNull.<Object>nullValue());
    }
}
