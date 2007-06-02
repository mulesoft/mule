package org.mule.providers.ssl;

import org.mule.extras.client.MuleClient;
import org.mule.impl.model.seda.SedaComponent;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.CounterCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.umo.UMOMessage;
import org.mule.umo.model.UMOModel;

import java.util.HashMap;
import java.util.Map;

public class SslFunctionalTestCase extends FunctionalTestCase {

    protected static String TEST_MESSAGE = "Test Request";
    private static int NUM_MESSAGES = 100;

    public SslFunctionalTestCase()
    {
        setDisposeManagerPerSuite(true);
    }

    protected String getConfigResources()
    {
        return "ssl-functional-test.xml";
    }

    public void testSend() throws Exception
    {
        MuleClient client = new MuleClient();
        Map props = new HashMap();
        UMOMessage result = client.send("sendEndpoint", TEST_MESSAGE, props);
        assertEquals(TEST_MESSAGE + " Received", result.getPayloadAsString());
    }

    public void testSendMany() throws Exception
    {
        MuleClient client = new MuleClient();
        Map props = new HashMap();
        for (int i = 0; i < NUM_MESSAGES; ++i)
        {
            UMOMessage result = client.send("sendManyEndpoint", TEST_MESSAGE, props);
            assertEquals(TEST_MESSAGE + " Received", result.getPayloadAsString());
        }

        UMOModel model = managementContext.getRegistry().lookupModel("main");
        SedaComponent seda = (SedaComponent) model.getComponent("testComponent2");
        assertNotNull("Null service", seda);
        FunctionalTestComponent ftc = (FunctionalTestComponent) seda.getInstance();
        assertNotNull("Null FTC", ftc);
        CounterCallback cc = (CounterCallback) ftc.getEventCallback();
        assertNotNull("Null CounterCallback", cc);
        assertEquals(NUM_MESSAGES, cc.getCallbackCount());
    }

    public void testAsynchronous() throws Exception
    {
        MuleClient client = new MuleClient();
        client.dispatch("asyncEndpoint", TEST_MESSAGE, null);
        UMOMessage response = client.receive("asyncEndpoint", 5000);
        assertNotNull("Response is null", response);
        assertEquals(TEST_MESSAGE + " Received Async", response.getPayloadAsString());
    }

}
