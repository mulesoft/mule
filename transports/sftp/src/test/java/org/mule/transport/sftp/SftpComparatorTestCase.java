/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import com.jcraft.jsch.SftpException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleException;
import org.mule.tck.functional.EventCallback;
import org.mule.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by christianlangmann on 06/12/14.
 */
public class SftpComparatorTestCase extends AbstractSftpTestCase {

    private static final long TIMEOUT = 30000;

    public static final String SFTP_CONNECTOR_NAME = "sftpConnector";
    public static final String FILE_NAMES[] = {"file01", "file02"};

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
                {ConfigVariant.FLOW, "mule-sftp-comparator-config-flow.xml"}
        });
    }

    public SftpComparatorTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Before
    public void setup() throws IOException, SftpException, MuleException {
        initEndpointDirectory("inboundEndpoint");
    }

    @After
    public void tearDown() throws IOException {
        recursiveDelete(getSftpClient("inboundEndpoint"), "inboundEndpoint", INBOUND_ENDPOINT_DIR);
    }

    @Override
    protected String getConfigFile()
    {
        return "mule-sftp-comparator-config-flow.xml";
    }

    public static String SFTP_HOST = "localhost";

    @Test
    public void testComparator() throws Exception
    {
        final CountDownLatch countDown = new CountDownLatch(2);
        EventCallback callback = new EventCallback()
        {
            @Override
            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {
                int index = (int) countDown.getCount() - 1;
                assertEquals(FILE_NAMES[index], context.getMessage().getInboundProperty(SftpConnector.PROPERTY_ORIGINAL_FILENAME));
                countDown.countDown();
            }
        };

        getFunctionalTestComponent("receiving").setEventCallback(callback);

        muleContext.getRegistry().lookupConnector(SFTP_CONNECTOR_NAME).stop();
        File f1 = FileUtils.newFile(sftpClient.getAbsolutePath("/~/" + INBOUND_ENDPOINT_DIR) + File.separator + FILE_NAMES[0]);
        assertTrue(f1.createNewFile());
        Thread.sleep(1000);
        File f2 = FileUtils.newFile(sftpClient.getAbsolutePath("/~/" + INBOUND_ENDPOINT_DIR) + File.separator + FILE_NAMES[1]);
        assertTrue(f2.createNewFile());
        Thread.sleep(1000);
        muleContext.getRegistry().lookupConnector(SFTP_CONNECTOR_NAME).start();
        assertTrue(countDown.await(TIMEOUT, TimeUnit.MILLISECONDS));
    }
}
