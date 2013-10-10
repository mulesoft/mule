/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.construct;

import java.io.File;

import junit.framework.Assert;

import org.junit.Test;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.FileUtils;

public class FlowSyncAsyncProcessingStrategyTestCase extends FunctionalTestCase
{

    public static final String SLEEP_TIME = "sleepTime";

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/construct/flow-sync-async-processing-strategy-config.xml";

    }

    @Test
    public void testSynchProcessingStrategy() throws Exception
    {
        sendMsgAndWait("vm://testSynch");

        File file = getFileTestWroteTo();
        String str = FileUtils.readFileToString(file);

        Assert.assertEquals("Part 1Part 2", str);

        FileUtils.deleteQuietly(file);
    }

    @Test
    public void testAsynch() throws Exception
    {
        sendMsgAndWait("vm://testAsynch");

        File file = getFileTestWroteTo();
        String str = FileUtils.readFileToString(file);

        Assert.assertEquals("Part 2Part 1", str);

        FileUtils.deleteQuietly(file);

    }

    private void sendMsgAndWait(String endpoint) throws Exception
    {
        MuleClient client = muleContext.getClient();

        client.dispatch(endpoint, "Part 1;Part 2", null);

        Thread.sleep(10000);

    }

    private File getFileTestWroteTo()
    {
        return new File("./test/testfile.txt");
    }

}
