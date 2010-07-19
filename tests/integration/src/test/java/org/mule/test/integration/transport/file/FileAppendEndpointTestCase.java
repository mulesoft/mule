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

import org.mule.module.client.MuleClient;
import org.mule.util.FileUtils;

import java.io.File;

public class FileAppendEndpointTestCase extends FileAppendConnectorTestCase
{
    @Override
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

        // output directory may not exist before dispatching to the endpoint with invalid
        // configuration
        File outputFile = FileUtils.newFile(myDir, myFileName);
        assertFalse(outputFile.exists());
        
        // this should throw java.lang.IllegalArgumentException: Configuring 'outputAppend' on a 
        // file endpoint is no longer supported. You may configure it on a file connector instead.
        MuleClient client = new MuleClient(muleContext);
        client.dispatch("vm://fileappend", "Hello1", null);

        assertFalse(outputFile.exists());
    }
    
    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/providers/file/mule-fileappend-endpoint-config.xml";
    }
}
