package org.mule.providers.soap.xfire;

import org.mule.extras.client.MuleClient;
import org.mule.impl.MuleMessage;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.testmodels.services.Person;
import org.mule.umo.UMOMessage;

public class ComplexTypeMethodTestCase extends FunctionalTestCase
{

    public void testSendComplexType() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage result = client.send("vm://inbound", new MuleMessage(new Person("Jane", "Doe")));
        assertNotNull(result.getPayload());
        assertTrue(result.getPayload() instanceof Boolean);
        assertTrue(((Boolean) result.getPayload()).booleanValue());
    }

    protected String getConfigResources()
    {
        return "xfire-complex-type-conf.xml";
    }
}
