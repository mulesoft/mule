/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.mule.api.FutureMessageResult;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.NullPayload;

import java.util.concurrent.TimeoutException;

import org.junit.Test;

public class HttpConnectionTimeoutTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "http-connection-timeout-config.xml";
    }

    @Test
    public void usesConnectionTimeout() throws Exception
    {

        final MuleClient client = new MuleClient(muleContext);
        FutureMessageResult result = client.sendAsync("vm://testInput", TEST_MESSAGE, null);

        MuleMessage message = null;
        try
        {
            message = result.getMessage(1000);
        }
        catch (TimeoutException e)
        {
            fail("Connection timeout not honored.");
        }

        assertEquals(NullPayload.getInstance(), message.getPayload());
        assertNotNull(message.getExceptionPayload());
    }
}
