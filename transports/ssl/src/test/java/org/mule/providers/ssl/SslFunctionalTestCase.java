package org.mule.providers.ssl;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

import java.util.HashMap;
import java.util.Map;

public class SslFunctionalTestCase extends FunctionalTestCase {

    protected static String TEST_MESSAGE = "Test SSL Request (R�dgr�d), 57 = \u06f7\u06f5 in Arabic";

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
        UMOMessage result = client.send("clientEndpoint", TEST_MESSAGE, props);
        assertEquals(TEST_MESSAGE + " Received", result.getPayloadAsString());
    }
    
}
