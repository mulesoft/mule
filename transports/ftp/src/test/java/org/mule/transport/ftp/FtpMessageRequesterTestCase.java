/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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


