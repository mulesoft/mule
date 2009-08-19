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
        return "ftp-message-requester-test.xml";
    }

    public void testMessageRequester() throws Exception
    {
        createFileOnFtpServer("test.txt");

        MuleClient client = new MuleClient();
        MuleMessage message = client.request(getMuleFtpEndpoint(), getTimeout());
        assertNotNull(message);
        assertEquals(TEST_MESSAGE, message.getPayloadAsString());
    }
}


