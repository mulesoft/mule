/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.shutdown;

import static org.junit.Assert.assertTrue;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.rule.SystemProperty;

import java.util.HashMap;

import org.junit.Rule;
import org.junit.Test;

public class ValidShutdownTimeoutOneWayTestCase extends AbstractShutdownTimeoutRequestResponseTestCase
{

    @Rule
    public SystemProperty contextShutdownTimeout = new SystemProperty("contextShutdownTimeout", "5000");

    @Override
    protected String getConfigResources()
    {
        return "shutdown-timeout-one-way-config.xml";
    }

    @Override
    protected boolean isGracefulShutdown()
    {
        return true;
    }

    @Test
    public void testStaticComponent() throws Exception
    {
        doShutDownTest("staticComponentResponse", "vm://staticComponent");
    }

    @Test
    public void testScriptComponent() throws Exception
    {
        doShutDownTest("scriptComponentResponse", "vm://scriptComponent");
    }

    @Test
    public void testExpressionTransformer() throws Exception
    {
        doShutDownTest("expressionTransformerResponse", "vm://expressionTransformer");
    }

    private void doShutDownTest(final String payload, final String url) throws MuleException, InterruptedException
    {
        final MuleClient client = new MuleClient(muleContext);
        final boolean[] results = new boolean[] {false};

        Thread t = new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    DefaultMuleMessage muleMessage = new DefaultMuleMessage(payload, new HashMap<String, Object>(), muleContext);
                    client.dispatch(url, muleMessage, null);

                    MuleMessage response = client.request("vm://response", RECEIVE_TIMEOUT);
                    results[0] = payload.equals(response.getPayloadAsString());
                }
                catch (Exception e)
                {
                    // Ignore
                }
            }
        };
        t.start();

        // Make sure to give the request enough time to get to the waiting portion of the feed.
        waitLatch.await();

        muleContext.stop();

        t.join();

        assertTrue("Was not able to process message ", results[0]);
    }

}
