/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.resolvers;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.model.resolvers.EntryPointNotFoundException;
import org.mule.tck.FunctionalTestCase;

public class MethodEntrypointsTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/test/integration/resolvers/method-entrypoints-config.xml";
    }

    public void testTooManySatifiableMethods() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage message = client.send("vm://service", "hello", null);
        assertNotNull(message.getExceptionPayload());
        assertTrue(message.getExceptionPayload().getException().getCause() instanceof EntryPointNotFoundException);
        assertTrue(message.getExceptionPayload().getException().getCause().getMessage().indexOf("Found too many possible methods on object") > -1);
    }

    public void testBadMethodName() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage message = client.send("vm://service?method=foo", "hello", null);
        assertNotNull(message.getExceptionPayload());
        assertTrue(message.getExceptionPayload().getException().getCause() instanceof EntryPointNotFoundException);
    }

    public void testValidCallToReverse() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage message = client.send("vm://service?method=reverseString", "hello", null);
        assertNotNull(message);
        assertEquals(message.getPayloadAsString(), "olleh");
    }

    public void testValidCallToUpperCase() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage message = client.send("vm://service?method=upperCaseString", "hello", null);
        assertNotNull(message);
        assertEquals(message.getPayloadAsString(), "HELLO");
    }
}
