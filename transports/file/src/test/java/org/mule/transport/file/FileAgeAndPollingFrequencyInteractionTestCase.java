/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mule.transport.file.FileTestUtils.createDataFile;

import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.transport.Connector;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.FileUtils;

import java.io.File;

import org.junit.Test;

public class FileAgeAndPollingFrequencyInteractionTestCase extends FunctionalTestCase
{
    private static File dataFile;
    private static boolean updateFileAge = true;

    public FileAgeAndPollingFrequencyInteractionTestCase()
    {
        setStartContext(false);
    }

    @Override
    protected String getConfigFile()
    {
        return "file-age-polling-config.xml";
    }

    @Test
    public void processesFileOnNextPollWhenFileIsOldEnough() throws Exception
    {
        File tmpDir = FileUtils.openDirectory(getFileInsideWorkingDirectory("in").getAbsolutePath());
        dataFile = createDataFile(tmpDir, TEST_MESSAGE, "UTF-8");

        muleContext.start();

        MuleMessage response = muleContext.getClient().request("vm://testOut", RECEIVE_TIMEOUT);

        assertNotNull("File was not processed", response);
        assertEquals(TEST_MESSAGE, response.getPayloadAsString());
    }

    public static class TestFileMessageReceiver extends FileMessageReceiver
    {

        public TestFileMessageReceiver(Connector connector, FlowConstruct flowConstruct, InboundEndpoint endpoint, String readDir, String moveDir, String moveToPattern, long frequency) throws CreateException
        {
            super(connector, flowConstruct, endpoint, readDir, moveDir, moveToPattern, frequency);
        }

        @Override
        public void poll()
        {
            super.poll();

            if (updateFileAge)
            {
                dataFile.setLastModified(System.currentTimeMillis() - 500000);
                updateFileAge = false;
            }
        }
    }
}
