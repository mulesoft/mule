/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.mule.api.MuleEventContext;
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

/** Test that OlderFirstComparator is used to sort and handle files by age and not by name, or other
 * SFTP-internal ordering.
 *
 * Created by christianlangmann on 06/12/14.
 */
public class SftpComparatorTestCase extends AbstractSftpTestCase {

    private static final long TIMEOUT = 30000;

    private static final String SFTP_CONNECTOR_NAME = "sftpConnector";
    private static final String SFTP_CONNECTOR_NAME_REV = "sftpConnectorRev";
    private static final String FILE_NAMES[] = {"3-file", "1-file", "2-file"}; // make sure files have a non-alphabetical order

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
    public void setup() throws Exception {
        initEndpointDirectory("inboundEndpoint"); // same for inboundEndpointReverse
        muleContext.getRegistry().lookupConnector(SFTP_CONNECTOR_NAME).stop();
        muleContext.getRegistry().lookupConnector(SFTP_CONNECTOR_NAME_REV).stop();
        for (final String filename : FILE_NAMES) {
            createFile(filename);
        }
    }

    private void createFile(final String filename) throws IOException, InterruptedException {
        final File f = FileUtils.newFile(sftpClient.getAbsolutePath("/~/" + INBOUND_ENDPOINT_DIR) + File.separator + filename);
        assertTrue(f.createNewFile());
        Thread.sleep(1000); // make sure f gets an older timestamp than next file
    }

    @After
    public void tearDown() throws IOException {
        recursiveDelete(getSftpClient("inboundEndpoint"), "inboundEndpoint", INBOUND_ENDPOINT_DIR);
    }

    @Test
    public void testComparator() throws Exception
    {
        runComparator(false);
    }

    @Test
    public void testReverseComparator() throws Exception
    {
        runComparator(true);
    }

    private void runComparator(final boolean reverse) throws Exception {
        final CountDownLatch countDown = new CountDownLatch(3);
        EventCallback callback = new EventCallback()
        {
            @Override
            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {
                int index = (int) (reverse ? countDown.getCount() - 1 : (FILE_NAMES.length - countDown.getCount()));
                assertEquals(FILE_NAMES[index], context.getMessage().getInboundProperty(SftpConnector.PROPERTY_ORIGINAL_FILENAME));
                countDown.countDown();
            }
        };

        getFunctionalTestComponent("receiving").setEventCallback(callback);
        final String connName = reverse ? SFTP_CONNECTOR_NAME_REV : SFTP_CONNECTOR_NAME;
        muleContext.getRegistry().lookupConnector(connName).start();
        assertTrue(countDown.await(TIMEOUT, TimeUnit.MILLISECONDS));
    }
}
