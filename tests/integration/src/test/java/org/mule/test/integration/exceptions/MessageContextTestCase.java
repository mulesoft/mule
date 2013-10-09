/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.exceptions;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertNotNull;

public class MessageContextTestCase extends AbstractServiceAndFlowTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/integration/exceptions/message-context-test-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/integration/exceptions/message-context-test-flow.xml"}
        });
    }

    public MessageContextTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    /**
     * Test for MULE-4361
     */
    @Test
    public void testAlternateExceptionStrategy() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        DefaultMuleMessage msg = new DefaultMuleMessage("Hello World", client.getMuleContext());
        MuleMessage response = client.send("testin", msg, 200000);
        assertNotNull(response);
    }
}
