/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.cookbook.quartz;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

/**
 * The Quartz transport can be used to trigger an event to be received by the
 * component based on the endpoint configuration. In Mule an event is usually
 * expected, however in this example we have a service component who's service method
 * doesn't take any parameters. The
 * {@link org.mule.transport.quartz.jobs.EventGeneratorJob} can be used to trigger a
 * service method, and by not specifying a 'payload' element there is no data to try
 * and match to the service method, so Mule will match a method with no arguments.
 */
public class TriggerNoArgsServiceMethodTestCase extends AbstractServiceAndFlowTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/cookbook/quartz/trigger-no-args-method-config-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/cookbook/quartz/trigger-no-args-method-config-flow.xml"}});
    }

    public TriggerNoArgsServiceMethodTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testTrigger() throws Exception
    {
        MuleClient client = muleContext.getClient();

        // Our method should have fired and we can pick up the result
        MuleMessage result = client.request("resultQueue", 2000);

        // Always check method is not null. It wuld be rude not to!
        assertNotNull(result);

        // Check we have a hit
        assertEquals("Bullseye!", result.getPayloadAsString());
    }
}
