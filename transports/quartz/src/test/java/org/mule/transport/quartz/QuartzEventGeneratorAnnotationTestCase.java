/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.quartz;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.NullPayload;

import org.junit.Ignore;
import org.junit.Test;

public class QuartzEventGeneratorAnnotationTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "quartz-event-generator-with-annotation.xml";
    }

    @Ignore("MULE-6926: flaky test")
    @Test
    public void testReceiveEvent() throws Exception
    {
        MuleClient client = muleContext.getClient();

        MuleMessage result = client.request("vm://intervalQueue", RECEIVE_TIMEOUT * 2);
        assertNotNull(result);
        assertFalse(result.getPayload() instanceof NullPayload);
        assertEquals("pinged! (interval)", result.getPayload());

        result = client.request("vm://cronQueue", RECEIVE_TIMEOUT);
        assertNotNull(result);
        assertFalse(result.getPayload() instanceof NullPayload);
        assertEquals("pinged! (cron)", result.getPayload());
    }
}
