/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.routing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.FileUtils;
import org.mule.util.SerializationUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import org.junit.Test;

public class UntilSuccessfulWithQueuePersistenceObjectStoreTestCase extends FunctionalTestCase
{

    public UntilSuccessfulWithQueuePersistenceObjectStoreTestCase()
    {
        setStartContext(false);
    }

    @Override
    protected String getConfigResources()
    {
        return "until-successful-with-queue-persistence-object-store-config.xml";
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
        File file = FileUtils.createFile(".mule/queuestore/queuestore/test1.msg");
        OutputStream os = new FileOutputStream(file);
        MuleEvent event = getTestEvent(TEST_MESSAGE);
        SerializationUtils.serialize(event, os);

        muleContext.start();
        LocalMuleClient client = muleContext.getClient();

        MuleMessage request = client.request("vm://dlq", RECEIVE_TIMEOUT);
        assertNotNull(request);
        assertEquals(TEST_MESSAGE, request.getPayloadAsString());
    }
}
