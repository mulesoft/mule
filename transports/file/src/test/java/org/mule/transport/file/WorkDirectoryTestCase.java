/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.file;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.api.MuleEventContext;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.util.FileUtils;

public class WorkDirectoryTestCase extends AbstractServiceAndFlowTestCase
{
    
    private static final String TEST_FILENAME = "test.txt";

    public WorkDirectoryTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }
    
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "work-directory-config-service.xml"},
            {ConfigVariant.FLOW, "work-directory-config-flow.xml"}
        });
    }

    @Override
    protected void doTearDown() throws Exception
    {
        // clean out the directory tree that's used as basis for this test
        File outputDir = getWorkingDirectory();
        assertTrue(FileUtils.deleteTree(outputDir));

        super.doTearDown();
    }

    @Test
    public void testWorkDirectory() throws Exception
    {
        FunctionalTestComponent ftc = (FunctionalTestComponent) getComponent("relay");
        ftc.setEventCallback(new EventCallback()
        {
            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {
                File workDir = getFileInsideWorkingDirectory("work");
                String[] filenames = workDir.list();
                assertTrue(filenames.length > 0);
                for (String filename : filenames)
                {
                    if (filename.contains(TEST_FILENAME))
                    {
                        return;
                    }
                }
                
                fail("no work dir file matching filename " + TEST_FILENAME);
            }
        });
        
        writeTestMessageToInputDirectory();        
        checkOutputDirectory();
    }

    private void writeTestMessageToInputDirectory() throws FileNotFoundException, IOException
    {
        File outFile = new File(getFileInsideWorkingDirectory("in"), TEST_FILENAME);
        FileOutputStream out = new FileOutputStream(outFile);
        out.write(TEST_MESSAGE.getBytes());
        out.close();
    }

    private void checkOutputDirectory() throws Exception
    {
        for (int i = 0; i < 30; i++)
        {
            File outDir = getFileInsideWorkingDirectory("out");
            if (outDir.exists())
            {
                String[] filenames = outDir.list();
                if (filenames.length > 0)
                {
                    for (String filename : filenames)
                    {
                        if (filename.contains(TEST_FILENAME))
                        {
                            return;
                        }
                    }
                }
            }
            
            Thread.sleep(1000);
        }

        fail("no file with name " + TEST_FILENAME + " in output directory");
    }
    
}
