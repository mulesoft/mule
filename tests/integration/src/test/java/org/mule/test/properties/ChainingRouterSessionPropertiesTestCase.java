package org.mule.test.properties;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

public class ChainingRouterSessionPropertiesTestCase extends FunctionalTestCase
{

    public static final String EXPECTED_MESSAGE = "First property value is sessionProp1Val other property value is sessionProp2Val.";

    @Override
	protected String getConfigResources()
    {
		return "org/mule/test/properties/chaining-router-session-properties.xml";
	}
	
	public void testSettingPropertyAfterCallingEndpoints() throws Exception {
		MuleClient client = new MuleClient(muleContext);
		MuleMessage msg = new DefaultMuleMessage("test", muleContext);
		msg = client.send("vm://Service1Request", msg);
		assertEquals(EXPECTED_MESSAGE, msg.getPayload());
		
	}
	
	public void testSettingPropertyBeforeCallingEndpoints() throws Exception
    {
		MuleClient client = new MuleClient(muleContext);
		MuleMessage msg = new DefaultMuleMessage("test", muleContext);
		msg = client.send("vm://Service2Request", msg);
		assertEquals(EXPECTED_MESSAGE, msg.getPayload());
	}

}
