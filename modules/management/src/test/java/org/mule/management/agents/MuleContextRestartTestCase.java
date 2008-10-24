package org.mule.management.agents;

import org.mule.tck.AbstractMuleTestCase;

public class MuleContextRestartTestCase extends AbstractMuleTestCase
{

    @Override
    protected String getConfigurationResources()
    {
        return "mule-context-restart-config.xml";
    }

    public void testContextRestart() throws Exception
    {
        muleContext.stop();
        muleContext.start();
    }
}
