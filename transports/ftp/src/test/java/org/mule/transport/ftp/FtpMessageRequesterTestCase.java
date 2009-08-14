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

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;

import java.io.OutputStream;
import java.net.URL;


public class FtpMessageRequesterTestCase extends AbstractFtpServerTestCase
{   
    private static final int PORT = 60199;
    
    public FtpMessageRequesterTestCase()
    {
        super(PORT);
    }

    @Override
    protected String getConfigResources()
    {
        return "ftp-functional-test.xml";
    }

    public void testMessageRequester() throws Exception
    {
        String url = "ftp://anonymous:email@localhost:" + PORT + "/test.txt";
        URL ftpUrl = new URL(url);

        // put a file on the FTP server
        OutputStream out = ftpUrl.openConnection().getOutputStream();
        out.write(TEST_MESSAGE.getBytes());
        out.close();
        
        MuleClient client = new MuleClient();
        MuleMessage message = client.request(url, getTimeout());
        assertNotNull(message);
        assertEquals(TEST_MESSAGE, message.getPayloadAsString());
    }
}


