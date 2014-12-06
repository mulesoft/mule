/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
=======
import com.jcraft.jsch.SftpException;
>>>>>>> 797ec72... SFTP supports comparator and reverse
=======
import com.jcraft.jsch.SftpException;
>>>>>>> 12a456f... SFTP supports comparator and reverse
=======
>>>>>>> 2ba8afd... Adjust tests so that order and reverse-order is tested
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.mule.api.MuleEventContext;
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
=======
import org.mule.api.MuleException;
>>>>>>> 797ec72... SFTP supports comparator and reverse
=======
import org.mule.api.MuleException;
>>>>>>> 12a456f... SFTP supports comparator and reverse
=======
>>>>>>> 2ba8afd... Adjust tests so that order and reverse-order is tested
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

<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
/** Test that OlderFirstComparator is used to sort and handle files by age and not by name, or other
 * SFTP-internal ordering.
 *
=======
/**
>>>>>>> 797ec72... SFTP supports comparator and reverse
=======
/**
>>>>>>> 12a456f... SFTP supports comparator and reverse
=======
/** Test that OlderFirstComparator is used to sort and handle files by age and not by name, or other
 * SFTP-internal ordering.
 *
>>>>>>> 2ba8afd... Adjust tests so that order and reverse-order is tested
 * Created by christianlangmann on 06/12/14.
 */
public class SftpComparatorTestCase extends AbstractSftpTestCase {

    private static final long TIMEOUT = 30000;

<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
    private static final String SFTP_CONNECTOR_NAME = "sftpConnector";
    private static final String SFTP_CONNECTOR_NAME_REV = "sftpConnectorRev";
    private static final String FILE_NAMES[] = {"3-file", "1-file", "2-file"}; // make sure files have a non-alphabetical order
=======
    public static final String SFTP_CONNECTOR_NAME = "sftpConnector";
    public static final String FILE_NAMES[] = {"file01", "file02"};
>>>>>>> 797ec72... SFTP supports comparator and reverse
=======
    public static final String SFTP_CONNECTOR_NAME = "sftpConnector";
    public static final String FILE_NAMES[] = {"file01", "file02"};
>>>>>>> 12a456f... SFTP supports comparator and reverse
=======
    private static final String SFTP_CONNECTOR_NAME = "sftpConnector";
<<<<<<< HEAD
    private static final String FILE_NAMES[] = {"file01", "file02"};
    private static final String SFTP_HOST = "localhost";
>>>>>>> 2e049a2... Introduce FileDescriptor for collecting SFTP-Files
=======
    private static final String SFTP_CONNECTOR_NAME_REV = "sftpConnectorRev";
    private static final String FILE_NAMES[] = {"3-file", "1-file", "2-file"}; // make sure files have a non-alphabetical order
>>>>>>> 2ba8afd... Adjust tests so that order and reverse-order is tested

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
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 2ba8afd... Adjust tests so that order and reverse-order is tested
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
<<<<<<< HEAD
=======
    public void setup() throws IOException, SftpException, MuleException {
        initEndpointDirectory("inboundEndpoint");
>>>>>>> 797ec72... SFTP supports comparator and reverse
=======
    public void setup() throws IOException, SftpException, MuleException {
        initEndpointDirectory("inboundEndpoint");
>>>>>>> 12a456f... SFTP supports comparator and reverse
=======
>>>>>>> 2ba8afd... Adjust tests so that order and reverse-order is tested
    }

    @After
    public void tearDown() throws IOException {
        recursiveDelete(getSftpClient("inboundEndpoint"), "inboundEndpoint", INBOUND_ENDPOINT_DIR);
    }

<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
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
=======
=======
>>>>>>> 12a456f... SFTP supports comparator and reverse
    @Override
    protected String getConfigFile()
=======
    @Test
    public void testComparator() throws Exception
>>>>>>> 2ba8afd... Adjust tests so that order and reverse-order is tested
    {
        runComparator(false);
    }

    @Test
    public void testReverseComparator() throws Exception
    {
<<<<<<< HEAD
        final CountDownLatch countDown = new CountDownLatch(2);
<<<<<<< HEAD
>>>>>>> 797ec72... SFTP supports comparator and reverse
=======
>>>>>>> 12a456f... SFTP supports comparator and reverse
=======
        runComparator(true);
    }

    private void runComparator(final boolean reverse) throws Exception {
        final CountDownLatch countDown = new CountDownLatch(3);
>>>>>>> 2ba8afd... Adjust tests so that order and reverse-order is tested
        EventCallback callback = new EventCallback()
        {
            @Override
            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
                int index = (int) (reverse ? countDown.getCount() - 1 : (FILE_NAMES.length - countDown.getCount()));
=======
                int index = (int) countDown.getCount() - 1;
>>>>>>> 797ec72... SFTP supports comparator and reverse
=======
                int index = (int) countDown.getCount() - 1;
>>>>>>> 12a456f... SFTP supports comparator and reverse
=======
                int index = (int) (reverse ? countDown.getCount() - 1 : (FILE_NAMES.length - countDown.getCount()));
>>>>>>> 2ba8afd... Adjust tests so that order and reverse-order is tested
                assertEquals(FILE_NAMES[index], context.getMessage().getInboundProperty(SftpConnector.PROPERTY_ORIGINAL_FILENAME));
                countDown.countDown();
            }
        };
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
        getFunctionalTestComponent("receiving").setEventCallback(callback);
        final String connName = reverse ? SFTP_CONNECTOR_NAME_REV : SFTP_CONNECTOR_NAME;
        muleContext.getRegistry().lookupConnector(connName).start();
=======
=======
>>>>>>> 12a456f... SFTP supports comparator and reverse

        getFunctionalTestComponent("receiving").setEventCallback(callback);

        muleContext.getRegistry().lookupConnector(SFTP_CONNECTOR_NAME).stop();
        File f1 = FileUtils.newFile(sftpClient.getAbsolutePath("/~/" + INBOUND_ENDPOINT_DIR) + File.separator + FILE_NAMES[0]);
        assertTrue(f1.createNewFile());
        Thread.sleep(1000);
        File f2 = FileUtils.newFile(sftpClient.getAbsolutePath("/~/" + INBOUND_ENDPOINT_DIR) + File.separator + FILE_NAMES[1]);
        assertTrue(f2.createNewFile());
        Thread.sleep(1000);
        muleContext.getRegistry().lookupConnector(SFTP_CONNECTOR_NAME).start();
<<<<<<< HEAD
>>>>>>> 797ec72... SFTP supports comparator and reverse
=======
>>>>>>> 12a456f... SFTP supports comparator and reverse
=======
        getFunctionalTestComponent("receiving").setEventCallback(callback);
        final String connName = reverse ? SFTP_CONNECTOR_NAME_REV : SFTP_CONNECTOR_NAME;
        muleContext.getRegistry().lookupConnector(connName).start();
>>>>>>> 2ba8afd... Adjust tests so that order and reverse-order is tested
        assertTrue(countDown.await(TIMEOUT, TimeUnit.MILLISECONDS));
    }
}
