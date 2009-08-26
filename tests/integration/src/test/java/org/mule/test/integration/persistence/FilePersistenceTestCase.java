/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.persistence;


import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.util.FileUtils;

import java.io.File;

public class FilePersistenceTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/test/integration/persistence/file-persistence-config.xml";
    }

    public void testFilesStored() throws Exception
    {
        // Note that the FunctionalTestCase will remove the working directory after
        // each execution
        String path = muleContext.getConfiguration().getWorkingDirectory() + "/queuestore/test.queue";
        File store = FileUtils.newFile(path);
        assertFalse(store.exists());

        MuleClient client = new MuleClient();
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
