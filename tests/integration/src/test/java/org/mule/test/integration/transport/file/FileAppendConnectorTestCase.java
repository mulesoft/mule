/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.transport.file;

import org.mule.api.context.notification.EndpointMessageNotificationListener;
import org.mule.context.notification.EndpointMessageNotification;
import org.mule.module.client.MuleClient;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

            MuleClient client = new MuleClient(muleContext);
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
