/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import org.junit.After;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.listener.FlowExecutionListener;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;
import org.mule.util.FileUtils;

import java.io.File;
import java.io.IOException;

import junit.framework.Assert;

import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowSyncAsyncProcessingStrategyTestCase extends FunctionalTestCase
{
    public static final String SLEEP_TIME = "sleepTime";
    private static final String FILE_PATH = "./test/testfile.txt";
    private File file;

    private static final Logger logger = LoggerFactory.getLogger(FlowSyncAsyncProcessingStrategyTestCase.class);

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/construct/flow-sync-async-processing-strategy-config.xml";
    }

    @After
    public void cleanUp()
    {
        FileUtils.deleteQuietly(file);
    }

    @Test
    public void testSynchProcessingStrategy() throws Exception
    {
        sendMessage("vm://testSynch");
        new FlowExecutionListener("synchFlow", muleContext).waitUntilFlowIsComplete();
        file = new File(FILE_PATH);
        String str = FileUtils.readFileToString(file);

        Assert.assertEquals("Part 1Part 2", str);
    }

    @Test
    public void testAsynch() throws Exception
    {
        sendMessage("vm://testAsynch");

        file = new File(FILE_PATH);
        Prober prober = new PollingProber(10000, 2000);
        prober.check(new FileCompleteProbe());
    }

    private void sendMessage(String endpoint) throws Exception
    {
        MuleClient client = muleContext.getClient();

        client.dispatch(endpoint, "Part 1;Part 2", null);
    }

    private class FileCompleteProbe implements Probe
    {
        private String output;

        public boolean isSatisfied()
        {
            if(file.exists())
            {
                try
                {
                    output = FileUtils.readFileToString(file);
                }
                catch (IOException e)
                {
                    logger.debug("Could not read from file.");
                }
                return "Part 2Part 1".equals(output);
            }
            else
            {
                return false;
            }
        }

        public String describeFailure()
        {
            return "Expected output was 'Part2Part 1' but actual one was: " + output;
        }
    }
}
