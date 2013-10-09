/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.sftp;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test the sizeCheck feature
 */
public class SftpSizeCheckFunctionalTestCase extends AbstractSftpTestCase
{

    private static final long TIMEOUT = 15000;

    // Size of the generated stream - 2 Mb
    final static int SEND_SIZE = 1024 * 1024 * 2;

    public SftpSizeCheckFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }
    
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "mule-sftp-sizeCheck-test-config-service.xml"},
            {ConfigVariant.FLOW, "mule-sftp-sizeCheck-test-config-flow.xml"}
        });
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        initEndpointDirectory("inboundEndpoint");
    }

    /**
     * Test the sizeCheck feature
     */
    @Test
    public void testSizeCheck() throws Exception
    {
        // TODO. Add some tests specific to sizeCheck, i.e. create a very large file
        // and ensure that the sizeChec prevents the inbound enpoint to read the file
        // during creation of it

        executeBaseTest("inboundEndpoint", "vm://test.upload", FILENAME, SEND_SIZE, "receiving", TIMEOUT);
    }
}
