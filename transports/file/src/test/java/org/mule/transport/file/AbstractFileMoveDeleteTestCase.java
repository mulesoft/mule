/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.file;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public abstract class AbstractFileMoveDeleteTestCase extends AbstractFileFunctionalTestCase
{

    protected File configureConnector(File inFile, boolean stream, boolean move, boolean delete)
        throws Exception
    {
        FileConnector fc = new FileConnector();
        fc.setName("moveDeleteConnector");
        File moveToDir = new File(inFile.getParent() + "/moveto/");
        moveToDir.mkdir();
        muleContext.getRegistry().registerConnector(fc);
        if (move)
        {
            fc.setMoveToDirectory(moveToDir.getPath());
        }
        fc.setAutoDelete(delete);
        fc.setStreaming(stream);
        return moveToDir;
    }

    protected void assertFiles(File inFile, File moveToDir, boolean move, boolean delete) throws Exception
    {
        waitForFileSystem();

        boolean inFileShouldExst = !delete && !move;

        assertTrue(inFile.exists() == inFileShouldExst);

        if (inFileShouldExst)
        {
            assertEquals(TEST_MESSAGE, new BufferedReader(new FileReader(inFile)).readLine());
        }

        File movedFile = new File(moveToDir.getPath() + "/" + inFile.getName());
        assertTrue(movedFile.exists() == move);

        if (move)
        {
            assertEquals(TEST_MESSAGE, new BufferedReader(new FileReader(movedFile)).readLine());
        }
    }

    protected void assertFilesUntouched(File inFile)
    {
        assertTrue(inFile.exists());

        File movedFile = new File(inFile.getParent() + "/moveto/" + inFile.getName());
        assertFalse(movedFile.exists());
    }

}
