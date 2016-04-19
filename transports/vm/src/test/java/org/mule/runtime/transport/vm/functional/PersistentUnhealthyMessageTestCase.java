/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.vm.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.util.FileUtils;
import org.mule.runtime.core.util.queue.DualRandomAccessFileQueueStoreDelegate;

import java.io.File;

import org.junit.Test;

public class PersistentUnhealthyMessageTestCase extends FunctionalTestCase
{

    public static final String OUTPUT_QUEUE_NAME = "flowOut";

    @Override
    protected String getConfigFile()
    {
        setStartContext(false);
        return "vm/persistent-vmqueue-test.xml";
    }

    @Test
    public void testUnhealthyMessageIgnored() throws Exception
    {
        File firstQueueFile = DualRandomAccessFileQueueStoreDelegate.getFirstQueueFileForTesting(OUTPUT_QUEUE_NAME, getWorkingDirectory().getAbsolutePath());
        FileUtils.createFile(firstQueueFile.getAbsolutePath());

        muleContext.start();

        MuleClient client = muleContext.getClient();
        client.dispatch("vm://flowIn", "echo", null);
        MuleMessage result = client.request("vm://" + OUTPUT_QUEUE_NAME, RECEIVE_TIMEOUT);
        assertNotNull(result);
        assertEquals("echo", result.getPayload());
    }
}


