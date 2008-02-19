
package org.mule.module.client;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.tck.AbstractMuleTestCase;

public class MuleClientStandaloneTestCase extends AbstractMuleTestCase
{

    protected MuleContext createMuleContext() throws Exception
    {
        return null;
    }

    public void testCreateMuleClient() throws MuleException
    {
        MuleClient muleClient = new MuleClient();
        assertNotSame(muleContext, muleClient.getMuleContext());
        assertTrue(muleClient.getMuleContext().isInitialised());
        assertTrue(muleClient.getMuleContext().isStarted());
        muleClient.dispatch("test://test", "message", null);
        muleClient.send("test://test", "message", null);
        muleClient.dispose();
        assertFalse(muleClient.getMuleContext().isInitialised());
        assertFalse(muleClient.getMuleContext().isStarted());
    }

}
