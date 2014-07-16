/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.transport.DispatchException;

import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.lang.NotImplementedException;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test the archive features.
 */
public class SftpDuplicateHandlingFunctionalTestCase extends AbstractSftpTestCase
{
    private static final long TIMEOUT = 10000;

    // Size of the generated stream - 2 Mb
    final static int SEND_SIZE = 1024 * 1024 * 2;

    public SftpDuplicateHandlingFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);

        // Only start mule once for all tests below, save a lot of time..., if test3
        // starts failing, comment this out
        setDisposeContextPerClass(true);

        // Increase the timeout of the test to 300 s
        logger.info("Timeout was set to: " + System.getProperty(TEST_TIMEOUT_SYSTEM_PROPERTY, "-1"));
        System.setProperty(TEST_TIMEOUT_SYSTEM_PROPERTY, "300000");
        logger.info("Timeout is now set to: " + System.getProperty(TEST_TIMEOUT_SYSTEM_PROPERTY, "-1"));
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "mule-sftp-duplicateHandling-test-config-service.xml"},
            {ConfigVariant.FLOW, "mule-sftp-duplicateHandling-test-config-flow.xml"}
        });
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        initEndpointDirectory("inboundEndpoint1");
        initEndpointDirectory("inboundEndpoint2");
        initEndpointDirectory("inboundEndpoint3");
        initEndpointDirectory("inboundEndpoint4");
        // FIXME: disabled due to missing config
        // initEndpointDirectory("outboundEndpoint5");

        muleContext.setExceptionListener(new org.mule.transport.sftp.notification.ExceptionListener());
    }

    /**
     * Test 1 - test duplicate handling by throwing an exception
     */
    @Test
    public void testDuplicateHandlingThrowException() throws Exception
    {
        // TODO. Add some tests specific to this test, i.e. not only rely on the
        // tests performed by executeTest().

        executeBaseTest("inboundEndpoint1", "vm://test.upload1", "file1.txt", SEND_SIZE, "receiving1",
            TIMEOUT, "sending1");
    }

    /**
     * Test 2 - test duplicate handling by overwriting the existing file Not yet
     * implemented, so currently we check for a valid exception...
     */
    @Test
    public void testDuplicateHandlingOverwrite() throws Exception
    {
        // TODO. Add some tests specific to this test, i.e. not only rely on the
        // tests performed by executeTest().

        try
        {
            executeBaseTest("inboundEndpoint2", "vm://test.upload2", "file2.txt", SEND_SIZE, "receiving2",
                TIMEOUT, "sftp", "sending2");
            fail("Should have received an Exception");
        }
        catch (Exception e)
        {
            assertTrue("did not receive DispatchException, got : " + e.getClass().toString(),
                e instanceof DispatchException);
            assertTrue(
                "did not receive NotImplementedException, got : " + e.getCause().getClass().toString(),
                e.getCause() instanceof NotImplementedException);
        }
    }

    /**
     * Test 3 - test duplicate handling by adding a sequence number to the new file
     */
    @Test
    public void testDuplicateHandlingAddSeqNo() throws Exception
    {
        // TODO. Add some tests specific to this test, i.e. not only rely on the
        // tests performed by executeTest().

        executeBaseTest("inboundEndpoint3", "vm://test.upload3", "file3.txt", SEND_SIZE, "receiving3",
            TIMEOUT, "receiving3");
    }

    /**
     * Test 4 - test duplicate handling by adding a sequence number to the new file
     * using default value on connector
     */
    @Test
    public void testDuplicateHandlingAddSeqNoUsingConnector() throws Exception
    {
        // TODO. Add some tests specific to this test, i.e. not only rely on the
        // tests performed by executeTest().

        executeBaseTest("inboundEndpoint4", "vm://test.upload4", "file4.txt", SEND_SIZE, "receiving4",
            TIMEOUT, "receiving4");

        ImmutableEndpoint endpoint = getImmutableEndpoint("send4outbound");
        SftpUtil util = new SftpUtil(endpoint);

        assertEquals("The value on the connector should be used", "addSeqNo", util.getDuplicateHandling());
    }

    /**
     * Test 5 - test duplicate handling by adding a sequence number to the new file
     * without file extension
     */
    /*
     * @Test
    public void testDuplicateHandlingAddSeqNoWithNoFileExtension() throws
     * Exception { MuleClient muleClient = muleContext.getClient(); HashMap<String, String>
     * txtProps = new HashMap<String, String>(1);
     * txtProps.put(SftpConnector.PROPERTY_FILENAME, "file5");
     * muleClient.dispatch("vm://test.upload5", TEST_MESSAGE, txtProps); // TODO:
     * make a executeBaseTest that doesn't require a FunctionalTestComponent
     * Thread.sleep(5000); // File #2 muleClient.dispatch("vm://test.upload5",
     * TEST_MESSAGE, txtProps); Thread.sleep(5000); SftpClient sftpClient = null; try
     * { sftpClient = getSftpClient(muleClient, "outboundEndpoint5");
     * ImmutableEndpoint endpoint = (ImmutableEndpoint)
     * muleClient.getProperty("outboundEndpoint5");
     * assertTrue("The file should exist in the directory",
     * verifyFileExists(sftpClient, endpoint.getEndpointURI(), "file5"));
     * assertTrue("The file should exist in the directory",
     * verifyFileExists(sftpClient, endpoint.getEndpointURI(), "file5_1")); } finally
     * { if (sftpClient != null) { sftpClient.disconnect(); } } }
     */
}
