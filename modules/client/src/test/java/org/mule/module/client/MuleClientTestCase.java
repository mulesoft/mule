
package org.mule.module.client;

import org.mule.api.MuleException;

public class MuleClientTestCase extends AbstractMuleClientTestCase
{

    public void testCreateMuleClient() throws MuleException
    {
        MuleClient muleClient = new MuleClient();
        assertEquals(muleContext, muleClient.getMuleContext());
        assertTrue(muleContext.isInitialised());
        assertTrue(muleContext.isStarted());
        muleClient.dispatch("test://test", "message", null);
        muleClient.send("test://test", "message", null);
        muleClient.dispose();
        assertTrue(muleClient.getMuleContext().isInitialised());
        assertTrue(muleClient.getMuleContext().isStarted());
    }

}
