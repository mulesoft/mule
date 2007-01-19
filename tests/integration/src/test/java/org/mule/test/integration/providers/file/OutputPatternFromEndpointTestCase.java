/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.providers.file;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

import java.io.File;

public class OutputPatternFromEndpointTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/test/integration/providers/file/mule-file-output-pattern-from-endpoint.xml";
    }

    public void testBasic() throws Exception
    {
        String myFirstDirName = "FirstWrite";
        String mySecondDirName = "SecondWrite";
        String myFileName1 = "export.txt";
        String myFileName2 = "export.txt.OK";

        // make sure there is no directory and file
        File myDir = new File(myFirstDirName);
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

        File myDir2 = new File(mySecondDirName);
        if (myDir2.isDirectory())
        {
            // Delete Any Existing Files
            File[] files = myDir2.listFiles();
            for (int i = 0; i < files.length; i++)
            {
                assertTrue(files[i].delete());
            }
            // This may fail if this directory contains other directories.
            assertTrue(myDir2.delete());
        }

        try
        {
            assertFalse(new File(myDir, myFileName1).exists());
            assertFalse(new File(myDir2, myFileName2).exists());

            MuleClient client = new MuleClient();
            client.send("vm://filesend", "Hello", null);

            // the output file should exist now
            // check that the files with the correct output pattern were generated
            assertTrue(new File(myDir, myFileName1).exists());
            assertTrue(new File(myDir2, myFileName2).exists());
        }
        finally
        {
            assertTrue(new File(myDir, myFileName1).delete());
            assertTrue(new File(myDir2, myFileName2).delete());
            assertTrue(myDir.delete());
            assertTrue(myDir2.delete());
        }
    }
}
