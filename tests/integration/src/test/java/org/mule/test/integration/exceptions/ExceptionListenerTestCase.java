/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.exceptions;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.message.ExceptionMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

public class ExceptionListenerTestCase extends FunctionalTestCase
{
    private MuleClient client;

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/exceptions/exception-listener-config.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        client = new MuleClient();
    }

    public void testExceptionStrategyFromComponent() throws Exception
    {
        assertQueueIsEmpty("vm://error.queue");

        client.send("vm://component.in", "test", null);
        
        assertQueueIsEmpty("vm://component.out");

        MuleMessage message = client.request("vm://error.queue", 2000);
        assertNotNull(message);
        Object payload = message.getPayload();
        assertTrue(payload instanceof ExceptionMessage);
    }

    private void assertQueueIsEmpty(String queueName) throws MuleException
    {
        MuleMessage message = client.request(queueName, 2000);
        assertNull(message);
    }
}
