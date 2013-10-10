/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.persistence;


import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.FileUtils;

import java.io.File;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class FilePersistenceTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/persistence/file-persistence-config.xml";
    }

    @Test
    public void testFilesStored() throws Exception
    {
        // Note that the FunctionalTestCase will remove the working directory after
        // each execution
        String path = muleContext.getConfiguration().getWorkingDirectory() + "/queuestore/test.queue";
        File store = FileUtils.newFile(path);
        assertFalse(store.exists());

        MuleClient client = new MuleClient(muleContext);
        client.dispatch("vm://test.queue", "test", null);
        // Give the vm dispatcher chance to persist message.  Cannot use send because send does not use queue.
        Thread.sleep(500);
        File[] files = store.listFiles();
        assertNotNull(files);
        assertEquals(1, files.length);

        muleContext.getRegistry().lookupService("TestComponent").start();
        // give the service some time to initialise
        Thread.sleep(2000);
        files = store.listFiles();
        assertNotNull(files);
        assertEquals(0, files.length);
    }
}
