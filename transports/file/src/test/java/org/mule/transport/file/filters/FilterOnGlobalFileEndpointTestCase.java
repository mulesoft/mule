/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.file.filters;

import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.util.FileUtils;

import java.io.File;
import java.io.IOException;

public class FilterOnGlobalFileEndpointTestCase extends FunctionalTestCase
{
    private static final String TEXT_FILE = "sample.txt";
    private static final String XML_FILE = "sample.xml";
    
    private File pollDirectory;

    @Override
    protected void doSetUp() throws Exception
    {
        createPollDirectoryAndInputFiles();
        super.doSetUp();
    }

    private void createPollDirectoryAndInputFiles() throws IOException
    {
        pollDirectory = createDirectory("target/testdir");
        createDirectory("target/testdir-moveto");

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

    @Override
    protected String getConfigResources()
    {
        return "global-file-ep-with-filter.xml";
    }

    public void testMoveFiles() throws Exception
    {
        File txtFile = new File(pollDirectory, TEXT_FILE);
        File xmlFile = new File(pollDirectory, XML_FILE);
        assertTrue(txtFile.exists());
        assertTrue(xmlFile.exists());
        
        MuleClient client = new MuleClient();
        client.request("globalEP", 1000);
        assertTrue(txtFile.exists());
        assertFalse(xmlFile.exists());
    }
    
}
