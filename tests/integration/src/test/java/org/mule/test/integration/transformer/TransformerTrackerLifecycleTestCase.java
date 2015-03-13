/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transformer;

import static org.junit.Assert.assertEquals;
import org.mule.api.MuleMessage;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class TransformerTrackerLifecycleTestCase extends AbstractServiceAndFlowTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/transformers/transformer-lifecycle-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/transformers/transformer-lifecycle-flow.xml"}});
    }

    public TransformerTrackerLifecycleTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testLifecycle() throws Exception
    {
        final MuleMessage result = muleContext.getClient().send("vm://EchoService.In", "foo", null);

        final LifecycleTrackerTransformer ltt = (LifecycleTrackerTransformer) result.getPayload();

        muleContext.dispose();

        assertEquals("[setProperty, setMuleContext, setMuleContext, initialise, setMuleContext, initialise, start, start, stop, stop, dispose]",
            ltt.getTracker().toString());
    }
}
