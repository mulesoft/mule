
package org.mule.extras.client;

import org.mule.api.MuleContext;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.UMOException;

public class MuleClientStandaloneTestCase extends AbstractMuleTestCase
{

    protected MuleContext createMuleContext() throws Exception
    {
        return null;
    }

    public void testCreateMuleClient() throws UMOException
    {
        MuleClient muleClient = new MuleClient();
        assertNotSame(muleContext, muleClient.getMuleContext());
        assertTrue(muleClient.getMuleContext().isInitialised());
        assertTrue(muleClient.getMuleContext().isStarted());
        muleClient.dispatch("test://test", "message", null);
        muleClient.send("test://test", "message", null);
        muleClient.dispose();
        // TODO MULE-2847
        //assertFalse(muleClient.getMuleContext().isInitialised());
        //assertFalse(muleClient.getMuleContext().isStarted());
    }

}
