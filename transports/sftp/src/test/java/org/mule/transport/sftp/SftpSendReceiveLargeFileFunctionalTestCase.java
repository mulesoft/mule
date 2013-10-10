/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test sending and receiving a very large message.
 * <p/>
 * This test will probably fail due to the standard timeout. According to
 * http://www.mulesource.org/display/MULE2USER/Functional+Testing the only way to
 * change the timeout is "add -Dmule.test.timeoutSecs=XX either to the mvn command
 * you use to run Mule or to the JUnit test runner in your IDE." Tested with
 * '-Dmule.test.timeoutSecs=300'
 */
public class SftpSendReceiveLargeFileFunctionalTestCase extends AbstractSftpTestCase
{
    private static final long TIMEOUT = 600000;

    // Size of the generated stream - 200 Mb
    final static int SEND_SIZE = 1024 * 1024 * 200;

    public SftpSendReceiveLargeFileFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);

        // Increase the timeout of the test to 300 s
        logger.info("Timeout was set to: " + System.getProperty(TEST_TIMEOUT_SYSTEM_PROPERTY, "-1"));
        System.setProperty(TEST_TIMEOUT_SYSTEM_PROPERTY, "600000");
        logger.info("Timeout is now set to: " + System.getProperty(TEST_TIMEOUT_SYSTEM_PROPERTY, "-1"));
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {        
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "mule-send-receive-large-file-test-config-service.xml"},
            {ConfigVariant.FLOW, "mule-send-receive-large-file-test-config-flow.xml"}});
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        initEndpointDirectory("inboundEndpoint");
    }

    /**
     * Test sending and receiving a large file.
     */
    @Test
    public void testSendAndReceiveLargeFile() throws Exception
    {
        executeBaseTest("inboundEndpoint", "vm://test.upload", "bigfile.txt", SEND_SIZE, "receiving", TIMEOUT);
    }
}
