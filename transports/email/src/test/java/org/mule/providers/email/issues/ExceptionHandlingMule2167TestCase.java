/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.email.issues;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

public class ExceptionHandlingMule2167TestCase extends FunctionalTestCase
{

    public static final String MESSAGE = "a message";
    public static final long WAIT_MS = 3000L;

    protected String getConfigResources()
    {
        return "exception-handling-mule-2167-test.xml";
    }

    public void testDefaultConfig() throws Exception
    {
        MuleClient client = new MuleClient();
        client.dispatch("vm://in-default?connector=defaultVm", MESSAGE, null);
        UMOMessage message = client.request("vm://out-default?connector=queue", WAIT_MS);
        assertNotNull("null message", message);
        assertNotNull("null payload", message.getPayload());
        assertEquals(MESSAGE, message.getPayloadAsString());
    }

}
