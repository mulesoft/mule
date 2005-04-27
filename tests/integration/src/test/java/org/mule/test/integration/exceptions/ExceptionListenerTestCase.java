package org.mule.test.integration.exceptions;

import org.mule.MuleManager;
import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.extras.client.MuleClient;
import org.mule.impl.message.ExceptionMessage;
import org.mule.tck.NamedTestCase;
import org.mule.umo.UMOMessage;

public class ExceptionListenerTestCase extends NamedTestCase
{
     public void setUp() throws Exception
    {
        if(MuleManager.isInstanciated()) MuleManager.getInstance().dispose();

        MuleXmlConfigurationBuilder builder = new MuleXmlConfigurationBuilder();
        builder.configure("org/mule/test/integration/exceptions/exception-listener-config.xml");
    }

    public void testExceptionStrategy() throws Exception
    {
        MuleClient client = new MuleClient();

        UMOMessage message = client.receive("vm://error.queue", 2000);
        assertNull(message);

        client.send("vm://mycomponent", "test", null);

        message = client.receive("vm://mycomponent.out", 2000);
        assertNull(message);

        message = client.receive("vm://error.queue", 2000);
        assertNotNull(message);
        Object payload = message.getPayload();
        assertTrue(payload instanceof ExceptionMessage);
    }
}
