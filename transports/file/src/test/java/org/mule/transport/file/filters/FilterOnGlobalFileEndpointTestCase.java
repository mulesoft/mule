/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.file.filters;

import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.FileUtils;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FilterOnGlobalFileEndpointTestCase extends FunctionalTestCase
{
    private static final String TEXT_FILE = "sample.txt";
    private static final String XML_FILE = "sample.xml";

    private File pollDirectory;

    @Override
    protected String getConfigResources()
    {
        return "global-file-ep-with-filter.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        createPollDirectoryAndInputFiles();
        super.doSetUp();
    }

    @Override
    protected void doTearDown() throws Exception
    {
        // discard the test directory structure
        assertTrue(FileUtils.deleteTree(pollDirectory.getParentFile()));

        super.doTearDown();
    }

    private void createPollDirectoryAndInputFiles() throws IOException
    {
        pollDirectory = createDirectory("target/FilterOnGlobalFileEndpointTestCase/testdir");
        createDirectory("target/FilterOnGlobalFileEndpointTestCase/testdir-moveto");

        createFileInPollDirectory(TEXT_FILE);
        createFileInPollDirectory(XML_FILE);
    }

    private File createDirectory(String path)
    {
        File directory = new File(path);
        if (directory.exists() == false)
        {
            if (directory.mkdirs() == false)
            {
                fail("could not create poll directory");
            }
        }

        return directory;
    }

    private void createFileInPollDirectory(String filename) throws IOException
    {
        File file  = FileUtils.newFile(pollDirectory, filename);

        String path = file.getCanonicalPath();

        File newFile = FileUtils.createFile(path);
        newFile.deleteOnExit();
    }

    @Test
    public void testMoveFiles() throws Exception
    {
        File txtFile = new File(pollDirectory, TEXT_FILE);
        File xmlFile = new File(pollDirectory, XML_FILE);
        assertTrue(txtFile.exists());
        assertTrue(xmlFile.exists());

        MuleClient client = new MuleClient(muleContext);
        client.request("globalEP", 1000);
        
        assertTrue(txtFile.exists());
        assertFalse(xmlFile.exists());
    }
}
