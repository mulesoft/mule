/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.components;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.transport.NullPayload;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ComponentReturningNullFlowTestCase extends AbstractServiceAndFlowTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/components/component-returned-null-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/components/component-returned-null-flow.xml"}});
    }

    public ComponentReturningNullFlowTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testNullReturnStopsFlow() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        MuleMessage msg = client.send("vm://in", "test data", null);
        assertNotNull(msg);
        final String payload = msg.getPayloadAsString();
        assertNotNull(payload);
        assertFalse("ERROR".equals(payload));
        assertTrue(msg.getPayload() instanceof NullPayload);
    }

    public static final class ComponentReturningNull
    {
        public String process(String input)
        {
            return null;
        }
    }
}
