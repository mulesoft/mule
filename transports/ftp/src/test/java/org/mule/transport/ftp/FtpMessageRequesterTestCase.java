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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class FtpMessageRequesterTestCase extends AbstractFtpServerTestCase
{

    public FtpMessageRequesterTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {               
        return Arrays.asList(new Object[][]{
            {ConfigVariant.FLOW, "ftp-message-requester-test.xml"}            
        });
    }      
    
    @Test
    public void testMessageRequester() throws Exception
    {
        createFileOnFtpServer("test.txt");

        MuleClient client = new MuleClient(muleContext);
        MuleMessage message = client.request(getMuleFtpEndpoint(), getTimeout());
        assertNotNull(message);
        assertEquals(TEST_MESSAGE, message.getPayloadAsString());

        // verify that the file was deleted
        MuleMessage message2 = client.request(getMuleFtpEndpoint(), getTimeout());
        assertNull(message2);
    }
}


