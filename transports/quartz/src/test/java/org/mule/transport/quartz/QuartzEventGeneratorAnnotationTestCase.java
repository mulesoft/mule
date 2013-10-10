/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.quartz;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.NullPayload;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class QuartzEventGeneratorAnnotationTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "quartz-event-generator-with-annotation.xml";
    }

    @Test
    public void testReceiveEvent() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

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
