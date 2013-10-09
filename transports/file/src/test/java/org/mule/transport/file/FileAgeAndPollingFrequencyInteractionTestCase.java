/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
    protected String getConfigResources()
    {
        return "file-age-polling-config.xml";
    }

    @Test
    public void processesFileOnNextPollWhenFileIsOldEnough() throws Exception
    {
        File tmpDir = FileUtils.openDirectory(".mule/in");
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
