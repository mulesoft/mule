/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.usecases.routing;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/*
 * In this Test Case we make use of a Custom Catch All Strategy in order to show how
 * to send the transformed message instead of the non-transformed message.
 */
public class InboundTransformingCatchAllTestCase extends AbstractServiceAndFlowTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{{ConfigVariant.SERVICE,
            "org/mule/test/usecases/routing/inbound-transforming-catchall-service.xml"}

        });
    }

    public InboundTransformingCatchAllTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }


    @Test
    public void testNormal() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        client.dispatch("vm://in1", new DefaultMuleMessage("HELLO!", muleContext));
        MuleMessage msg = client.request("vm://catchall", 3000);
        assertNotNull(msg);
        assertTrue(msg.getPayload() instanceof String);

        client.dispatch("vm://in2", new DefaultMuleMessage("HELLO!", muleContext));
        msg = client.request("vm://catchall", 3000);
        assertNotNull(msg);
        assertTrue(msg.getPayload() instanceof byte[]);
    }
}
