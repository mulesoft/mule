//COPYRIGHT
package org.mule.test.usecases.routing.response;

import org.mule.MuleManager;
import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.extras.client.MuleClient;
import org.mule.tck.NamedTestCase;
import org.mule.umo.UMOMessage;

//AUTHOR

public class ResponseAggregatorTestCase extends NamedTestCase {

    protected void setUp() throws Exception
    {
        if(MuleManager.isInstanciated()) MuleManager.getInstance().dispose();
        MuleXmlConfigurationBuilder builder = new MuleXmlConfigurationBuilder();
        builder.configure("org/mule/test/usecases/routing/response/response-router.xml");
    }

    public void testSyncResponse() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage message = client.send("http://localhost:8081", "request", null);
        assertNotNull(message);
        assertEquals("Received: request", new String(message.getPayloadAsBytes()));
    }
}
