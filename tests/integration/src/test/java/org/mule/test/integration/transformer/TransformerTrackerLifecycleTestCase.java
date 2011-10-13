/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transformer;

import static org.junit.Assert.assertEquals;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class TransformerTrackerLifecycleTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/transformers/transformer-lifecycle.xml";
    }

    @Test
    public void testLifecycle() throws Exception
    {

        MuleClient muleClient = muleContext.getClient();

        final MuleMessage result = muleClient.send("vm://EchoService.In", "foo", null);

        final LifecycleTrackerTransformer ltt = (LifecycleTrackerTransformer) result.getPayload();

        muleContext.dispose();

        //TODO MULE-5002 initialise called twice
        assertEquals("[setProperty, setMuleContext, setMuleContext, initialise, start, stop]", 
            ltt.getTracker().toString());
    }
}
