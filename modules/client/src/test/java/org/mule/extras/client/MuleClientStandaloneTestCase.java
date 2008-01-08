
package org.mule.extras.client;

import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.UMOException;
import org.mule.umo.UMOManagementContext;

public class MuleClientStandaloneTestCase extends AbstractMuleTestCase
{

    protected UMOManagementContext createManagementContext() throws Exception
    {
        return null;
    }

    public void testCreateMuleClient() throws UMOException
    {
        MuleClient muleClient = new MuleClient();
        assertNotSame(managementContext, muleClient.getManagementContext());
        assertTrue(muleClient.getManagementContext().isInitialised());
        assertTrue(muleClient.getManagementContext().isStarted());
        muleClient.dispatch("test://test", "message", null);
        muleClient.send("test://test", "message", null);
        muleClient.dispose();
        // TODO MULE-2847
        //assertFalse(muleClient.getManagementContext().isInitialised());
        //assertFalse(muleClient.getManagementContext().isStarted());
    }

}
