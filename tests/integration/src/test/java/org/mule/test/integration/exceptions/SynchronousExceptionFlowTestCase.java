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

public class SynchronousExceptionFlowTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "org/mule/test/integration/exceptions/synchronous-exception-handler-config.xml";
    }

    public void testMuleFlow() throws Exception
    {
        MuleClient client = new MuleClient();
        String myDyingWords = "Gazpacho Soup!";
        MuleMessage result = client.send("vm://mule2.in", myDyingWords, null);
        Object payload = result.getPayload();
        assertTrue(payload instanceof ErrorResponse);
        String payloadDescription = ((ErrorResponse) payload).getDescription();
        assertEquals("java.lang.Exception: " + myDyingWords, payloadDescription);
    }
}
