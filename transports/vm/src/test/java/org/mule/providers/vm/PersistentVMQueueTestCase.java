package org.mule.providers.vm;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

/**
 *
 */

public class PersistentVMQueueTestCase extends FunctionalTestCase
{
    private static final int RECEIVE_TIMEOUT = 5000;

    protected String getConfigResources()
    {
        return "persistent-vmqueue-test.xml";
    }

    public void testAsynchronousDispatching() throws Exception
    {
        String input = "Test message";
        String[] output = {"Test", "message"};
        MuleClient client = new MuleClient();
        client.dispatch("vm://receiver", input, null);
        UMOMessage result = client.receive("vm://out", RECEIVE_TIMEOUT);
        assertNotNull(result);
        assertNotNull(result.getPayload());
        assertNull(result.getExceptionPayload());
        String[] payload = (String[]) result.getPayload();
        assertEquals(output.length, payload.length);
        for (int i = 0; i < output.length; i++)
        {
            assertEquals(output[i], payload[i]);
        }
    }

}
