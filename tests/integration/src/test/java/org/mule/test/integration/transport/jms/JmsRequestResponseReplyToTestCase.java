/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transport.jms;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.NullPayload;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;

/**
 * Test that static outbound endpoints behave in the same way when
 * they are preceded by a JMS inbound endpoint.
 */
public class JmsRequestResponseReplyToTestCase extends AbstractServiceAndFlowTestCase
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    public JmsRequestResponseReplyToTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {ConfigVariant.SERVICE, "org/mule/test/integration/transport/jms/jms-request-response-reply-to-config-service.xml"},
                {ConfigVariant.FLOW, "org/mule/test/integration/transport/jms/jms-request-response-reply-to-config-flow.xml"}});
    }

    @Test
    public void testStaticHttpOutboundRepliesToJmsInbound() throws Exception
    {
        doTest("jms://jms.static");
    }

    @Test
    public void testDynamicHttpOutboundRepliesToJmsInbound() throws Exception
    {
        doTest("jms://jms.dynamic");
    }

    private void doTest(String jmsRequestUrl) throws MuleException
    {
        MuleClient client = muleContext.getClient();

        MuleMessage result = client.send(jmsRequestUrl, "localhost:" + dynamicPort.getNumber() + "/test", null);

        assertNotNull(result);
        assertNull(result.getExceptionPayload());
        assertFalse("Response payload shouldn't be null", result.getPayload() instanceof NullPayload);
    }
}
