/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.vm.functional;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

public class PersistentUnaddressedVmQueueTestCase extends FunctionalTestCase
{
    private static final int RECEIVE_TIMEOUT = 5000;

    protected String getConfigResources()
    {
        return "vm/persistent-unaddressed-vm-queue-test.xml";
    }

    public void testAsynchronousDispatching() throws Exception
    {
        MuleClient client = new MuleClient();
        client.dispatch("vm://receiver1?connector=Connector1", "Test", null);
        MuleMessage result = client.request("vm://out?connector=Connector2", RECEIVE_TIMEOUT);
	    assertNotNull(result);
	    assertEquals(result.getPayloadAsString(),"Test");
    }

}
