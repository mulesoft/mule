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

import org.mule.api.MuleMessage;
import org.mule.tck.FunctionalTestCase;
import org.mule.util.FileUtils;
import org.mule.util.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.Writer;
import java.net.MalformedURLException;

/**
 * We are careful here to access the file system in a generic way. This means setting
 * directories dynamically.
 */
public abstract class AbstractFileFunctionalTestCase extends FunctionalTestCase
{

    public static final String TEST_MESSAGE = "Test file contents";
    public static final String TARGET_FILE = "TARGET_FILE";

    private File tmpDir;

    public AbstractFileFunctionalTestCase()
    {
        setDisposeManagerPerSuite(false);
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
        synchronized (this)
        {
            wait(1000);
        }
    }

    protected File initForRequest() throws Exception
    {
        tmpDir = File.createTempFile("mule-file-test-", "-dir");
        tmpDir.delete();
        tmpDir.mkdir();
        tmpDir.deleteOnExit();
        File target = File.createTempFile("mule-file-test-", ".txt", tmpDir);
        Writer out = new FileWriter(target);
        out.write(TEST_MESSAGE);
        out.close();
        target.deleteOnExit();

        // define the readFromDirectory on the connector
        FileConnector connector = (FileConnector) muleContext.getRegistry().lookupConnector(
            "receiveConnector");
        connector.setReadFromDirectory(tmpDir.getAbsolutePath());
        logger.debug("Directory is " + connector.getReadFromDirectory());

        waitForFileSystem();
        return target;
    }

    protected void checkReceivedMessage(MuleMessage message) throws Exception
    {
        assertNotNull(message);
        assertNotNull(message.getPayload());
        assertTrue(message.getPayload() instanceof InputStream);

        InputStream fis = (InputStream) message.getPayload();
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        IOUtils.copy(fis, byteOut);
        fis.close();
        String result = new String(byteOut.toByteArray());
        assertEquals(TEST_MESSAGE, result);
    }

    protected void doTearDown() throws Exception
    {
        super.doTearDown();
        FileUtils.deleteTree(tmpDir);
    }

}
