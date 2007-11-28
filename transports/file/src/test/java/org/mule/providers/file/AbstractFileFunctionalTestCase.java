/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.file;

import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.net.MalformedURLException;

/**
 * We are careful here to access the file system in a generic way.  This means setting directories
 * dynamically.
 */
public abstract class AbstractFileFunctionalTestCase extends FunctionalTestCase
{

    public static final String TEST_MESSAGE = "Test file contents";
    public static final String TARGET_FILE = "TARGET_FILE";

    public AbstractFileFunctionalTestCase()
    {
        setDisposeManagerPerSuite(true);
    }

    protected String getConfigResources()
    {
        return "file-functional-test.xml";
    }

    protected String fileToUrl(File file) throws MalformedURLException
    {
        return file.getAbsoluteFile().toURI().toURL().toString();
    }

    // annoying but necessary wait apparently due to OS caching?
    protected void waitForFileSystem() throws Exception
    {
        synchronized(this)
        {
            wait(1000);
        }
    }

    protected File initForRequest() throws Exception
    {
        File tmpDir = File.createTempFile("mule-file-test-", "-dir");
        tmpDir.delete();
        tmpDir.mkdir();
        tmpDir.deleteOnExit();
        File target = File.createTempFile("mule-file-test-", ".txt", tmpDir);
        Writer out = new FileWriter(target);
        out.write(TEST_MESSAGE);
        out.close();
        target.deleteOnExit();

        // define the readFromDirectory on the connector
        FileConnector connector =
                (FileConnector) managementContext.getRegistry().lookupConnector("receiveConnector");
        connector.setReadFromDirectory(tmpDir.getAbsolutePath());
        logger.debug("Directory is " + connector.getReadFromDirectory());

        waitForFileSystem();
        return target;
    }

    protected void checkReceivedMessage(UMOMessage message) throws Exception
    {
        assertNotNull(message);
        assertNotNull(message.getPayload());
        assertTrue(message.getPayload() instanceof byte[]);
        String result = new String((byte[]) message.getPayload());
        assertEquals(TEST_MESSAGE, result);
    }

}