/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MPL style
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.sftp;

/**
 * Test the sizeCheck feature.
 */
public class SftpSizeCheckFunctionalTestCase extends AbstractSftpTestCase
{
    private static final long TIMEOUT = 15000;

    // Size of the generated stream - 2 Mb
    final static int SEND_SIZE = 1024 * 1024 * 2;

    protected String getConfigResources()
    {
        return "mule-sftp-sizeCheck-test-config.xml";
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
    public void testSizeCheck() throws Exception
    {
        // TODO. Add some tests specific to sizeCheck, i.e. create a very large file
        // and ensure that the sizeChec prevents the inbound enpoint to read the file
        // during creation of it

        executeBaseTest("inboundEndpoint", "vm://test.upload", FILE_NAME, SEND_SIZE, "receiving", TIMEOUT);
    }
}
