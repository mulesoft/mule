/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.ftp;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;

import java.io.File;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

public class FtpMessageDispatcherTestCase extends AbstractFtpServerTestCase
{
    private static final int PORT = 61099;
    
    private CountDownLatch latch;
    
    public FtpMessageDispatcherTestCase()
    {
        super(PORT);        
        latch = new CountDownLatch(1);
    }

    @Override
    protected String getConfigResources()
    {
        return "ftp-message-requester-test.xml";
    }

    public void testDispatch() throws Exception
    {
        MuleClient client = new MuleClient();
        client.dispatch(getMuleFtpEndpoint(), new DefaultMuleMessage(TEST_MESSAGE));
        
        // check that the message arrived on the FTP server
        assertTrue(latch.await(getTimeout(), TimeUnit.MILLISECONDS));

        String[] filesOnServer = new File(FTP_SERVER_BASE_DIR).list();
        assertTrue(filesOnServer.length > 0);
    }
    
    public void testSend() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage result = client.send(getMuleFtpEndpoint(), new DefaultMuleMessage(TEST_MESSAGE));
        assertNotNull(result);
        
        // check that the message arrived on the FTP server
        assertTrue(latch.await(getTimeout(), TimeUnit.MILLISECONDS));

        String[] filesOnServer = new File(FTP_SERVER_BASE_DIR).list();
        assertTrue(filesOnServer.length > 0);
    }

    @Override
    public void fileUploadCompleted()
    {
        latch.countDown();
    }
}


