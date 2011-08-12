/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
