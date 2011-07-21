/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.ftp;

import static org.junit.Assert.assertTrue;

import org.mule.DefaultMuleMessage;
import org.mule.module.client.MuleClient;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class FtpMessageDispatcherTestCase extends AbstractFtpServerTestCase
{
    public FtpMessageDispatcherTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    private CountDownLatch latch = new CountDownLatch(1);

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{            
            {ConfigVariant.FLOW, "ftp-message-requester-test.xml"}
        });
    }      
    
    @Test
    public void testDispatch() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        client.dispatch(getMuleFtpEndpoint(), new DefaultMuleMessage(TEST_MESSAGE, muleContext));

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


