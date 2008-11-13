/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.components;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.NullPayload;

public class ComponentReturningNullFlowTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/test/components/component-returned-null.xml";
    }

    public void testNullReturnStopsFlow() throws Exception
    {
        MuleClient client = new MuleClient();

        MuleMessage msg = client.send("vm://in", "test data", null);
        assertNotNull(msg);
        final String payload = msg.getPayloadAsString();
        assertNotNull(payload);
        assertFalse("ERROR".equals(payload));
        assertTrue(msg.getPayload() instanceof NullPayload);
    }

    public static final class ComponentReturningNull
    {
        public String process(String input)
        {
            return null;
        }
    }
}
