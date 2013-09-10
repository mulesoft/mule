/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transport.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.mule.api.client.MuleClient;
import org.mule.api.context.notification.EndpointMessageNotificationListener;
import org.mule.context.notification.EndpointMessageNotification;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.util.FileUtils;
import org.mule.util.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class FileAppendConnectorTestCase extends AbstractServiceAndFlowTestCase implements EndpointMessageNotificationListener<EndpointMessageNotification>
{
    protected static final String OUTPUT_DIR = "myout";
    protected static final String OUTPUT_FILE = "out.txt";

    protected CountDownLatch fileReceiveLatch = new CountDownLatch(2);

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/integration/providers/file/mule-fileappend-connector-config-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/integration/providers/file/mule-fileappend-connector-config-flow.xml"}
        });
    }

    public FileAppendConnectorTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        muleContext.registerListener(this);
    }

    @Override
    protected void doTearDown() throws Exception
    {
        File outputDir = FileUtils.newFile(OUTPUT_DIR);
        FileUtils.deleteTree(outputDir);

        super.doTearDown();
    }

    @Test
    public void testBasic() throws Exception
    {
        FileInputStream myFileStream = null;
        try
        {
            File myDir = FileUtils.newFile(OUTPUT_DIR);
            File myFile = FileUtils.newFile(myDir, OUTPUT_FILE);
            assertFalse(myFile.exists());

            MuleClient client = muleContext.getClient();
            client.send("vm://fileappend", "Hello1", null);
            client.send("vm://fileappend", "Hello2", null);

            assertTrue(fileReceiveLatch.await(30, TimeUnit.SECONDS));

            // the output file should exist now
            myFileStream = new FileInputStream(myFile);
            assertEquals("Hello1Hello2", IOUtils.toString(myFileStream));
        }
        finally
        {
            IOUtils.closeQuietly(myFileStream);
        }
    }

    @Override
    public void onNotification(EndpointMessageNotification notification)
    {
        if (notification.getEndpoint().contains("myout"))
        {
            fileReceiveLatch.countDown();
        }
    }
}
