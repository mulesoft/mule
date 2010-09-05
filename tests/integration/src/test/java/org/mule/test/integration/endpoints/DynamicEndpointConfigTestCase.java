package org.mule.test.integration.endpoints;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.DataType;
import org.mule.tck.FunctionalTestCase;

/**
 *
 */
public class DynamicEndpointConfigTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/endpoints/dynamic-endpoint-config.xml";
    }

    public void testName() throws Exception
    {

        MuleMessage msg = new DefaultMuleMessage("Data", muleContext);
        msg.setOutboundProperty("testProp", "testPath");
        MuleMessage response = muleContext.getClient().send("vm://in1", msg);
        assertNotNull(response);
        assertNull(response.getExceptionPayload());
        assertEquals("Data Received", response.getPayload(DataType.STRING_DATA_TYPE));

        response = muleContext.getClient().send("vm://in2", msg);
        assertNotNull(response);
        assertNull(response.getExceptionPayload());
        assertEquals("Data Received", response.getPayload(DataType.STRING_DATA_TYPE));
    }
}
