/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.usecases.dlq;

import org.mule.extras.client.MuleClient;
import org.mule.impl.message.ExceptionMessage;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;


public class DLQExceptionHandlerTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "org/mule/test/usecases/dlq/exception-dlq.xml";
    }

    public void testDLQ() throws Exception
    {
        MuleClient client = new MuleClient();
        client.getManagementContext().start();
        client.dispatch("jms://request.queue", "testing 1 2 3", null);

        UMOMessage message = client.receive("jms://out.queue", 3000);
        assertNull(message);

        try
        {
            message = client.receive("jms://DLQ", 20000);
        }
        catch (UMOException e)
        {
            e.printStackTrace(System.err);
        }
        assertNotNull(message);

        ExceptionMessage em = (ExceptionMessage)message.getPayload();
        assertEquals("testing 1 2 3", em.getPayload());
    }
}
