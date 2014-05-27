/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class KeepOriginalFilePropertiesTestCase extends FunctionalTestCase
{
    private static final String OUT_QUEUE = "vm://file.outbox";
    private static final String TEST_FILENAME = "original.txt";
    private static final String PROCESSED_PREFIX = "processed-";
    private static final String INPUT_DIRECTORY = "input";
    private static final String OUTPUT_DIRECTORY = "output";
    private static final String CUSTOM_PROPERTY_ORIGINAL_FILENAME = "aux-" + FileConnector.PROPERTY_ORIGINAL_FILENAME;
    private static final String CUSTOM_PROPERTY_ORIGINAL_DIRECTORY = "aux-" + FileConnector.PROPERTY_ORIGINAL_DIRECTORY;
    private static final String PRIVATE_PATH = "/private";

    @Test
    public void moveToPatternWithDirectory() throws Exception
    {
        MuleMessage msg = waitUntilMessageIsProcessed();
        assertNotNull(msg);

        assertPropertiesAvailableAtPatternResolution(msg);

        assertEquals(TEST_FILENAME, getProperty(msg, CUSTOM_PROPERTY_ORIGINAL_FILENAME));

        assertOriginalDirectoryIsCorrectlySet(msg);
    }

    private MuleMessage waitUntilMessageIsProcessed() throws MuleException
    {
        return muleContext.getClient().request(OUT_QUEUE, 3000);
    }

    private String getProperty(MuleMessage msg, String propertyName)
    {
        return msg.getInboundProperty(propertyName);
    }

    private void assertPropertiesAvailableAtPatternResolution(MuleMessage msg)
    {
        assertTrue(new File(getFileInsideWorkingDirectory(OUTPUT_DIRECTORY), PROCESSED_PREFIX + getProperty(msg, CUSTOM_PROPERTY_ORIGINAL_FILENAME)).exists());
    }

    private void assertOriginalDirectoryIsCorrectlySet(MuleMessage msg) throws IOException
    {
        String originalDirectory = getFileInsideWorkingDirectory(INPUT_DIRECTORY).getPath();
        String originalDirectoryPropertyValue = getProperty(msg, CUSTOM_PROPERTY_ORIGINAL_DIRECTORY);

        assertEquals(disambiguateFie(originalDirectory), disambiguateFie(originalDirectoryPropertyValue));
    }

    private String disambiguateFie(String path) throws IOException
    {
        return new File(path).getCanonicalFile().getAbsolutePath();
    }

    private String removePrivatePath(String path)
    {
        if(path.startsWith(PRIVATE_PATH))
        {
            path = path.substring(PRIVATE_PATH.length());
        }
        return path;
    }

    @After
    public void clearDirectories()
    {
        assertTrue(FileUtils.deleteTree(getWorkingDirectory()));
    }

    @Before
    public void writeTestMessageToInputDirectory() throws IOException
    {
        File outFile = new File(getFileInsideWorkingDirectory(INPUT_DIRECTORY), TEST_FILENAME);
        FileOutputStream out = new FileOutputStream(outFile);
        try
        {
            out.write(TEST_MESSAGE.getBytes());
        }
        finally
        {
            out.close();
        }
    }

    @Override
    protected String getConfigFile()
    {
        return "keep-original-file-properties-config.xml";
    }
}
