/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

public class NoArgsCallWrapperFunctionalTestCase extends FunctionalTestCase
{
    private static final int RECEIVE_TIMEOUT = 5000;

    protected String getConfigResources()
    {
        return "no-args-call-wrapper-config.xml";
    }

    public void testNoArgsCallWrapper() throws Exception
    {
        MuleClient client = new MuleClient();
        client.dispatch("vm://invoke", "test", null);
        UMOMessage reply = client.receive("vm://out", RECEIVE_TIMEOUT);
        assertNotNull(reply);
        assertNull(reply.getExceptionPayload());
        assertEquals("Just an apple.", reply.getPayload());
    }

    public void testWithInjectedDelegate() throws Exception
    {
        MuleClient client = new MuleClient();
        client.dispatch("vm://invokeWithInjected", "test", null);
        UMOMessage reply = client.receive("vm://outWithInjected", RECEIVE_TIMEOUT);
        assertNotNull(reply);
        assertNull(reply.getExceptionPayload());
        // same as original input
        assertEquals("test", reply.getPayload());
    }

}
