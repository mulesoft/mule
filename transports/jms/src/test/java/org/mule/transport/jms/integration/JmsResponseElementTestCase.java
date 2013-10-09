/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.integration;

import org.hamcrest.core.Is;
import org.hamcrest.core.IsNull;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.transport.PropertyScope;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

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
        MuleClient client = new MuleClient(muleContext);
        MuleMessage response = client.send("vm://vminbound", "some message", null);
        assertThat(response.getPayloadAsString(), is(EXPECTED_MODIFIED_MESSAGE));
        assertThat(response.<String>getProperty("test", PropertyScope.INBOUND), Is.is("test"));
        assertThat(response.getExceptionPayload(), IsNull.<Object>nullValue());
    }

    @Test
    public void testInboundEndpointResponse() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage response = client.send("vm://vminbound2", MESSAGE, null);
        assertThat(response.getPayloadAsString(), is(EXPECTED_MODIFIED_MESSAGE));
        assertThat(response.getExceptionPayload(), IsNull.<Object>nullValue());
    }

    @Test
    public void testInboundEndpointResponseWithReplyTo() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        Map messageProperties = new HashMap();
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
        MuleClient client = new MuleClient(muleContext);
        MuleMessage response = client.send("jms://in3", MESSAGE, null);
        assertThat(response.getPayloadAsString(), is(EXPECTED_MODIFIED_MESSAGE));
        assertThat(response.getExceptionPayload(), IsNull.<Object>nullValue());
    }



}

