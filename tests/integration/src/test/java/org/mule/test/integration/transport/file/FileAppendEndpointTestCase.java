/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.transport.file;

import org.mule.api.transport.DispatchException;
import org.mule.module.client.MuleClient;
import org.mule.util.FileUtils;

import java.io.File;

public class FileAppendEndpointTestCase extends FileAppendConnectorTestCase
{
    public void testBasic() throws Exception
    {
        String myDirName = "myout";
        String myFileName = "out.txt";

        // make sure there is no directory and file
        File myDir = FileUtils.newFile(myDirName);
        if (myDir.isDirectory())
        {
            // Delete Any Existing Files
            File[] files = myDir.listFiles();
            for (int i = 0; i < files.length; i++)
            {
                assertTrue(files[i].delete());
            }
            // This may fail if this directory contains other directories.
            assertTrue(myDir.delete());
        }
        try
        {
            assertFalse(FileUtils.newFile(myDir, myFileName).exists());

            MuleClient client = new MuleClient();
            client.send("vm://fileappend", "Hello1", null);
            fail("Expected exception: java.lang.IllegalArgumentException: configuring outputAppend on the file endpoint is no longer support. You can configure a the File connector instead.");
        }
        catch (Exception e)
        {
            // java.lang.IllegalArgumentException: configuring outputAppend on the
            // file endpoint is no longer support. You can configure a the File
            // connector instead.
            assertEquals(DispatchException.class, e.getClass());
        }

    }

    protected String getConfigResources()
    {
        return "org/mule/test/integration/providers/file/mule-fileappend-endpoint-config.xml";
    }
}
