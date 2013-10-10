/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.quartz;

import static org.junit.Assert.assertNotNull;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class QuartzJobExceptionTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "quartz-job-exception-config.xml";
    }

    @Test
    public void invokesFlowExceptionStrategy() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        MuleMessage response = client.request("vm://error", RECEIVE_TIMEOUT);

        assertNotNull("Flow exception strategy was not invoked", response);
    }
}
