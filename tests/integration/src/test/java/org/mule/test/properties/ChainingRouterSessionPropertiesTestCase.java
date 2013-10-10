/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.properties;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ChainingRouterSessionPropertiesTestCase extends FunctionalTestCase
{

    public static final String EXPECTED_MESSAGE = "First property value is sessionProp1Val other property value is sessionProp2Val.";

    @Override
	protected String getConfigResources()
    {
		return "org/mule/test/properties/chaining-router-session-properties.xml";
	}

    @Test
	public void testSettingPropertyAfterCallingEndpoints() throws Exception {
		MuleClient client = new MuleClient(muleContext);
		MuleMessage msg = new DefaultMuleMessage("test", muleContext);
		msg = client.send("vm://Service1Request", msg);
		assertEquals(EXPECTED_MESSAGE, msg.getPayload());
	}

    @Test
	public void testSettingPropertyBeforeCallingEndpoints() throws Exception
    {
		MuleClient client = new MuleClient(muleContext);
		MuleMessage msg = new DefaultMuleMessage("test", muleContext);
		msg = client.send("vm://Service2Request", msg);
		assertEquals(EXPECTED_MESSAGE, msg.getPayload());
	}

}
