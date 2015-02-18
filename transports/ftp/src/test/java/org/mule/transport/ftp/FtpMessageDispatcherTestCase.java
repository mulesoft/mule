/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ftp;

import static org.junit.Assert.assertTrue;
import org.mule.api.MuleEvent;
import org.mule.api.client.MuleClient;
import org.mule.api.transport.OutputHandler;
import org.mule.util.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class FtpMessageDispatcherTestCase extends AbstractFtpServerTestCase
{
    private CountDownLatch latch = new CountDownLatch(1);

    public FtpMessageDispatcherTestCase(ConfigVariant variant, String configResources)
    {  
        super(variant, configResources);
    }
    
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {            
            {ConfigVariant.FLOW, "ftp-message-requester-test.xml"}
        });
    }      
    
    @Test
    public void dispatch() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.dispatch(getMuleFtpEndpoint(), getTestMuleMessage(TEST_MESSAGE));

        // check that the message arrived on the FTP server
        assertTrue(latch.await(getTimeout(), TimeUnit.MILLISECONDS));

        String[] filesOnServer = new File(FTP_SERVER_BASE_DIR).list();
        assertTrue(filesOnServer.length > 0);
    }

    @Test
    public void dispatchWithOutputHandler() throws Exception
    {
        MuleClient client = muleContext.getClient();
        OutputHandler oh = new OutputHandler()
        {
            public void write(MuleEvent event, OutputStream out) throws IOException
            {
            	StringReader sr = new StringReader(TEST_MESSAGE);
                try
                {
                    IOUtils.copy(sr, out);
                    out.flush();
                }
                finally
                {
                    sr.close();
                }
            }
        };
        client.dispatch(getMuleFtpEndpoint(), getTestMuleMessage(oh));

        // check that the message arrived on the FTP server
        assertTrue(latch.await(getTimeout(), TimeUnit.MILLISECONDS));

        String[] filesOnServer = new File(FTP_SERVER_BASE_DIR).list();
        assertTrue(filesOnServer.length > 0);
    }
    
    @Test
    public void dispatchToPath() throws Exception
    {
        String dirName = "test_dir";

        File testDir = new File(FTP_SERVER_BASE_DIR, dirName);
        testDir.deleteOnExit();
        assertTrue(testDir.mkdir());

        MuleClient client = muleContext.getClient();
        String path = getMuleFtpEndpoint() + "/" + dirName;
        client.dispatch(path, getTestMuleMessage(TEST_MESSAGE));

        // check that the message arrived on the FTP server
        assertTrue(latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));

        String[] filesOnServer = testDir.list();
        assertTrue(filesOnServer.length > 0);
    }
    
    @Override
    public void fileUploadCompleted()
    {
        latch.countDown();
    }
}
