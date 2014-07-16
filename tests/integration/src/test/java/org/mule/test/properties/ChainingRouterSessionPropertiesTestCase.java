/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.properties;

import static org.junit.Assert.assertEquals;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class ChainingRouterSessionPropertiesTestCase extends FunctionalTestCase
{
    public static final String EXPECTED_MESSAGE = "First property value is sessionProp1Val other property value is sessionProp2Val.";

    @Override
	protected String getConfigFile()
    {
		return "org/mule/test/properties/chaining-router-session-properties.xml";
	}

    @Test
    public void testSettingPropertyAfterCallingEndpoints() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage msg = new DefaultMuleMessage("test", muleContext);
        msg = client.send("vm://Service1Request", msg);
        assertEquals(EXPECTED_MESSAGE, msg.getPayload());
    }

    @Test
	public void testSettingPropertyBeforeCallingEndpoints() throws Exception
    {
        MuleClient client = muleContext.getClient();
		MuleMessage msg = new DefaultMuleMessage("test", muleContext);
		msg = client.send("vm://Service2Request", msg);
		assertEquals(EXPECTED_MESSAGE, msg.getPayload());
	}
}
