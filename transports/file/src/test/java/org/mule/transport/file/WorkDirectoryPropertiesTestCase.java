/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.mule.api.MuleMessage;
import org.mule.tck.junit4.FunctionalTestCase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class WorkDirectoryPropertiesTestCase extends FunctionalTestCase
{

    private File dataFolder;

    public WorkDirectoryPropertiesTestCase()
    {
        setStartContext(false);
    }

    @Override
    protected String getConfigResources()
    {
        return "work-directory-properties-config.xml";
    }

    @Before
    public void createDataFolder() throws Exception
    {
        dataFolder = new File(muleContext.getConfiguration().getWorkingDirectory(), "data");

        if (!dataFolder.exists())
        {
            assertTrue("Unable to create test folder", dataFolder.mkdirs());
        }
    }

    @Test
    public void testName() throws Exception
    {
        File testfile = createTestFile(dataFolder, "sample.txt");

        muleContext.start();

        MuleMessage response = muleContext.getClient().request("vm://testOut", RECEIVE_TIMEOUT * 6);

        assertTrue(response.getPayload() instanceof Map);
        Map<String, String> payload = (Map<String, String>) response.getPayload();
        assertEquals(dataFolder.getCanonicalPath(), payload.get(FileConnector.PROPERTY_SOURCE_DIRECTORY));
        assertEquals(testfile.getName(), payload.get(FileConnector.PROPERTY_SOURCE_FILENAME));
    }

    private File createTestFile(File parentFolder, String fileName) throws IOException
    {
        File result = new File(parentFolder, fileName);

        FileOutputStream out = new FileOutputStream(result);
        out.write(TEST_MESSAGE.getBytes());
        out.close();

        return result;
    }
}
