/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.file;

import org.mule.transport.AbstractMuleMessageFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public abstract class AbstractFileMoveDeleteTestCase extends AbstractFileFunctionalTestCase
{

    public AbstractFileMoveDeleteTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    protected File configureConnector(File inFile, boolean stream, boolean move, boolean delete,
                                      Class<? extends AbstractMuleMessageFactory> messageFactoryClass) throws Exception
    {
        return configureConnector(inFile, stream, move, delete, false, messageFactoryClass);
    }

    protected File configureConnector(File inFile, boolean stream, boolean move, boolean delete,
        boolean useWorkDir, Class<? extends AbstractMuleMessageFactory> messageFactoryClass) throws Exception
    {
        FileConnector fc = new FileConnector(muleContext);
        
        // some tests assert that a sinlge message arrives from this connector. Use a very large
        // polling frequency to avoid multiple messages coming in during a single test run.
        fc.setPollingFrequency(3000000);
        
        if (messageFactoryClass != null)
        {
            Map<String, String> overrides = new HashMap<String, String>();
            overrides.put("message.factory", messageFactoryClass.getName());
            fc.setServiceOverrides(overrides);
        }
        
        fc.setName("moveDeleteConnector");
        File moveToDir = new File(inFile.getParent() + "/moveto/");
        moveToDir.mkdir();
        File workDir = new File(inFile.getParent() + "/work/");
        workDir.mkdir();
        muleContext.getRegistry().registerConnector(fc);
        if (move)
        {
            fc.setMoveToDirectory(moveToDir.getPath());
        }
        if (useWorkDir)
        {
            fc.setWorkDirectory(workDir.getPath());
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
