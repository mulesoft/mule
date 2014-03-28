/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.api.store.ObjectStore;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class UntilSuccessfulWithPersistedQueueTestCase extends FunctionalTestCase
{
    public UntilSuccessfulWithPersistedQueueTestCase()
    {
        setStartContext(false);
    }

    @Override
    protected String getConfigFile()
    {
        return "until-successful-with-persisted-queue-config.xml";
    }

    @Test
    public void sendsMessageToDlqAfterExceededRetries() throws Exception
    {
        muleContext.start();
        LocalMuleClient client = muleContext.getClient();
        client.send("vm://testInput", TEST_MESSAGE, null);

        MuleMessage request = client.request("vm://dlq", RECEIVE_TIMEOUT);
        assertNotNull(request);
        assertEquals(TEST_MESSAGE, request.getPayloadAsString());
    }

    @Test
    public void recoversFromPersistedQueue() throws Exception
    {
        MuleEvent event = getTestEvent(TEST_MESSAGE);
        ObjectStore os = muleContext.getRegistry().lookupObject("objectStore");
        os.store("1", event);

        muleContext.start();
        LocalMuleClient client = muleContext.getClient();

        MuleMessage request = client.request("vm://dlq", RECEIVE_TIMEOUT);
        assertNotNull(request);
        assertEquals(TEST_MESSAGE, request.getPayloadAsString());
    }
}
