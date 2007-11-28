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

import org.mule.extras.client.MuleClient;
import org.mule.impl.message.ExceptionMessage;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

public class ExceptionListenerTestCase extends FunctionalTestCase
{

    public ExceptionListenerTestCase()
    {
        setDisposeManagerPerSuite(true);
    }

    protected String getConfigResources()
    {
        return "org/mule/test/integration/exceptions/exception-listener-config.xml";
    }

    public void testExceptionStrategyFromComponent() throws Exception
    {
        MuleClient client = new MuleClient();

        UMOMessage message = client.request("vm://error.queue", 2000);
        assertNull(message);

        client.send("vm://component.in", "test", null);

        message = client.request("vm://component.out", 2000);
        assertNull(message);

        message = client.request("vm://error.queue", 2000);
        assertNotNull(message);
        Object payload = message.getPayload();
        assertTrue(payload instanceof ExceptionMessage);
    }

    public void testExceptionStrategyForTransformerException() throws Exception
    {
        MuleClient client = new MuleClient();

        UMOMessage message = client.request("vm://error.queue", 2000);
        assertNull(message);

        client.send("vm://component.in", "test", null);

        message = client.request("vm://component.out", 2000);
        assertNull(message);

        message = client.request("vm://error.queue", 2000);
        assertNotNull(message);
        Object payload = message.getPayload();
        assertTrue(payload instanceof ExceptionMessage);
    }

    public void testExceptionStrategyForTransformerExceptionAsync() throws Exception
    {
        MuleClient client = new MuleClient();

        UMOMessage message = client.request("vm://error.queue", 2000);
        assertNull(message);

        client.dispatch("vm://component.in", "test", null);

        message = client.request("vm://component.out", 2000);
        assertNull(message);

        message = client.request("vm://error.queue", 2000);
        assertNotNull(message);
        Object payload = message.getPayload();
        assertTrue(payload instanceof ExceptionMessage);
    }
}
