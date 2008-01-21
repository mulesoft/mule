
package org.mule.extras.client;

import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.UMOException;

public class MuleClientTestCase extends AbstractMuleTestCase
{

    public void testCreateMuleClient() throws UMOException
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
