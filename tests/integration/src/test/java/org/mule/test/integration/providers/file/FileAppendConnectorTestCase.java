/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.providers.file;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.util.FileUtils;

import java.io.File;
import java.io.FileInputStream;

import org.apache.commons.io.IOUtils;

public class FileAppendConnectorTestCase extends FunctionalTestCase
{

    public void testBasic() throws Exception
    {
        String myDirName = "myout";
        String myFileName = "out.txt";
        FileInputStream myFileStream = null;

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
            client.send("vm://fileappend", "Hello2", null);

            // the output file should exist now
            myFileStream = new FileInputStream(FileUtils.newFile(myDir, myFileName));
            assertEquals("Hello1Hello2", IOUtils.toString(myFileStream));
        }
        finally
        {
            IOUtils.closeQuietly(myFileStream);
            assertTrue(FileUtils.newFile(myDir, myFileName).delete());
            assertTrue(myDir.delete());
        }
    }

    protected String getConfigResources()
    {
        return "org/mule/test/integration/providers/file/mule-fileappend-endpoint-config.xml";
    }
}
