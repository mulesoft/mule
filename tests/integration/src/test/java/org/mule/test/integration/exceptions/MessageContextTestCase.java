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

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

public class MessageContextTestCase extends FunctionalTestCase 
{
	String request = "Hello World";

	@Override
    protected String getConfigResources() 
	{
		return "org/mule/test/integration/exceptions/message-context-test.xml";
	}

	/**
	 * Test for MULE-4361
	 */
	public void testAlternateExceptionStrategy() throws Exception 
	{
	    MuleClient client = new MuleClient();
	    DefaultMuleMessage msg = new DefaultMuleMessage(request, client.getMuleContext());
	    MuleMessage response = client.send("testin", msg, 200000);
	    assertNotNull(response);
	    Thread.sleep(10000); // Wait for test to finish

	}
}
