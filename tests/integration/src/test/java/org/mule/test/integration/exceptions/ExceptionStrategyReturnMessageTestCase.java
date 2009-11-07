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

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

public class ExceptionStrategyReturnMessageTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/exceptions/exception-strategy-return-message.xml";
    }

    public void testExceptionMessage() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage msg = client.send("vm://in", "Test Message", null);

        assertNotNull(msg);
        assertNotNull(msg.getExceptionPayload());
        assertEquals("Functional Test Service Exception", msg.getExceptionPayload().getMessage());

        assertNotNull(msg.getPayload());
        assertEquals("Ka-boom!", msg.getPayload());
    }
}
