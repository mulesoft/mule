/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.quartz;

import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Ignore;
import org.junit.Test;

public class QuartzJobExceptionTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "quartz-job-exception-config.xml";
    }

    @Test
    @Ignore("MULE-6926: Flaky test.")
    public void invokesFlowExceptionStrategy() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        MuleMessage response = client.request("vm://error", RECEIVE_TIMEOUT);
        assertNotNull("Flow exception strategy was not invoked", response);
    }
}
