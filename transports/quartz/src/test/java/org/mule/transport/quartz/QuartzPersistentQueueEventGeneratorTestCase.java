/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.quartz;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.NullPayload;

public class QuartzPersistentQueueEventGeneratorTestCase extends FunctionalTestCase
{
    private static final long TIMEOUT = 30000;

    @Override
    protected String getConfigResources()
    {
        return "quartz-persistent-event-generator.xml";
    }
    
    public void testReceiveEvent() throws Exception
    {
        MuleClient client = new MuleClient();
        
        MuleMessage result = client.request("vm://resultQueue", TIMEOUT);
        assertNotNull(result);
        assertFalse(result.getPayload() instanceof NullPayload);
        assertEquals(TEST_MESSAGE, result.getPayload());
    }    
}


