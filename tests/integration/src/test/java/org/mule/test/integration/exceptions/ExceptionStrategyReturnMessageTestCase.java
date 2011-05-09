/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.exceptions;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

// TODO This test case is illogical because if the request is end-to-end synchronous, no exception strategy 
// will be called, the exception is simple returned to the client.
public class ExceptionStrategyReturnMessageTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/exceptions/exception-strategy-return-message.xml";
    }

    public void testExceptionMessage() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage msg = client.send("vm://in", "Test Message", null);

        assertNotNull(msg);
        assertNotNull(msg.getExceptionPayload());
        assertEquals("Functional Test Service Exception", msg.getExceptionPayload().getMessage());

        assertNotNull(msg.getPayload());
        assertEquals("Ka-boom!", msg.getPayload());
    }
}
