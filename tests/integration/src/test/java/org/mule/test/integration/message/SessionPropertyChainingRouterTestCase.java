package org.mule.test.integration.message;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.NonSerializableObject;

/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

/**
 * @see EE-1821
 */
public class SessionPropertyChainingRouterTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/messaging/session-property-chaining-router.xml";
    }

    public void testRouter() throws Exception
    {
    	MuleClient client = new MuleClient();
    	MuleMessage response = client.send("vm://in", "test message", null);
    	assertNotNull(response);
    	assertTrue("Reponse is " + response.getPayload(), response.getPayload() instanceof NonSerializableObject);
    }
}


