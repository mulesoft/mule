/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.vm.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.util.FileUtils;
import org.mule.util.SerializationUtils;
import org.mule.util.queue.DelegateQueueManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import org.junit.Rule;
import org.junit.Test;

public class PersistentRecoverMessageTestCase extends FunctionalTestCase
{

    @Rule
    public SystemProperty useOldQueueMode = new SystemProperty(DelegateQueueManager.MULE_QUEUE_OLD_MODE_KEY, "true");

    public PersistentRecoverMessageTestCase()
    {
        setStartContext(false);
    }

    @Override
    protected String getConfigFile()
    {
        return "vm/persistent-vmqueue-test.xml";
    }

    @Test
    public void testRecoverMessage() throws Exception
    {
        File file = FileUtils.createFile(".mule/queuestore/flowOut/0-000-out-01.msg");
        OutputStream os = new FileOutputStream(file);
        MuleEvent event = getTestEvent("echo");
        SerializationUtils.serialize(event, os);
        muleContext.start();

        MuleClient client = muleContext.getClient();
        MuleMessage result = client.request("vm://flowOut", RECEIVE_TIMEOUT);
        assertNotNull(result);
        assertEquals("echo", result.getPayload());
    }
}


