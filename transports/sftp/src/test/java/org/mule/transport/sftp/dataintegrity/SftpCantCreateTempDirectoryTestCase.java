/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp.dataintegrity;

import static org.junit.Assert.assertTrue;

import org.mule.api.client.MuleClient;
import org.mule.transport.sftp.LatchDownExceptionListener;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests that files are not deleted if the temp directory can't be created
 */
public class SftpCantCreateTempDirectoryTestCase extends AbstractSftpDataIntegrityTestCase
{
    private static final int EXCEPTION_TIMEOUT = 3000;
    private static String INBOUND_ENDPOINT_NAME = "inboundEndpoint";

    public SftpCantCreateTempDirectoryTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "dataintegrity/sftp-dataintegrity-common-with-tempdir-config-service.xml"},
            {ConfigVariant.FLOW, "dataintegrity/sftp-dataintegrity-common-with-tempdir-config-flow.xml"}});
    }

    @Override
    protected void doSetUpBeforeMuleContextCreation() throws Exception
    {
        super.doSetUpBeforeMuleContextCreation();
        sftpClient.mkdir(INBOUND_ENDPOINT_DIR);
        sftpClient.mkdir(OUTBOUND_ENDPOINT_DIR);
    }

    // Commented because it's failing even though the test is excluded
    //@After
    public void after() throws Exception
    {
        Runtime.getRuntime().exec(new String[]{"chmod", "777", OUTBOUND_ENDPOINT_DIR});
        sftpClient.recursivelyDeleteDirectory(INBOUND_ENDPOINT_DIR);
        sftpClient.recursivelyDeleteDirectory(OUTBOUND_ENDPOINT_DIR);
        sftpClient.disconnect();
    }

    /**
     * No write access on the outbound directory and thus the TEMP directory cant be
     * created. The source file should still exist
     */
    @Test
    public void testCantCreateTempDirectory() throws Exception
    {
        Runtime.getRuntime().exec(new String[]{"chmod", "320", OUTBOUND_ENDPOINT_DIR});
        final CountDownLatch exceptionLatch = new CountDownLatch(1);
        muleContext.registerListener(new LatchDownExceptionListener(exceptionLatch));

        MuleClient muleClient = muleContext.getClient();
        muleClient.dispatch("sftp://localhost:" + port.getNumber() + "/" + INBOUND_ENDPOINT_NAME,
            TEST_MESSAGE, MESSAGE_PROPERTIES);
        assertTrue(exceptionLatch.await(EXCEPTION_TIMEOUT, TimeUnit.MILLISECONDS));
        assertTrue(Arrays.asList(sftpClient.listFiles()).contains(FILENAME));
    }
}
