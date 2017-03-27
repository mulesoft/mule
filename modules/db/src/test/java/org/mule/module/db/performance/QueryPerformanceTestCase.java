/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.performance;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.api.client.LocalMuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;

/**
 * Not a real test, used to generate load and verify that there are no memory
 * leaks using a profiler.
 */
@Ignore
public class QueryPerformanceTestCase extends FunctionalTestCase
{

    private static final Logger LOGGER = getLogger(QueryPerformanceTestCase.class);
    private LoadGenerator loadGenerator = new LoadGenerator();

    @Override
    protected String getConfigFile()
    {
        return "integration/derby-datasource.xml,integration/select/default-select-config.xml";
    }

    @Override
    public int getTestTimeoutSecs()
    {
        return 5 * 60;
    }

    @Test
    public void testRequestResponsePerformance() throws Exception
    {
        loadGenerator.generateLoad(new RequestResponseLoadTask());
        takeANap();
    }

    @Test
    public void testOneWayPerformance() throws Exception
    {
        Thread outputCleaner = new Thread(new LoadCleaner());
        outputCleaner.start();
        loadGenerator.generateLoad(new OneWayLoadTask());
        takeANap();
        outputCleaner.interrupt();
    }

    private void takeANap() throws InterruptedException
    {
        Thread.sleep(2 * 60 * 1000);
    }

    private static class LoadCleaner implements Runnable
    {

        @Override
        public void run()
        {
            LocalMuleClient client = muleContext.getClient();
            while (!Thread.currentThread().isInterrupted())
            {
                try
                {
                    client.request("vm://testOut", RECEIVE_TIMEOUT);
                }
                catch (Exception e)
                {
                    // Ignore
                }
            }
        }
    }

    private class RequestResponseLoadTask implements LoadTask
    {

        @Override
        public void execute(int messageId) throws Exception
        {
            LOGGER.info("Thread: " + Thread.currentThread().getName() + " message: " + messageId);
            LocalMuleClient client = muleContext.getClient();

            client.send("vm://testRequestResponse", TEST_MESSAGE, null);
        }
    }

    private class OneWayLoadTask implements LoadTask
    {

        @Override
        public void execute(int messageId) throws Exception
        {
            LOGGER.info("Thread: " + Thread.currentThread().getName() + " message: " + messageId);
            LocalMuleClient client = muleContext.getClient();

            client.dispatch("vm://testOneWay", TEST_MESSAGE, null);
        }
    }

}
