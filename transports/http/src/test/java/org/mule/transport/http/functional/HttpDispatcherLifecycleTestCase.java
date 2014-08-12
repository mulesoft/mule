/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.functional;


import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class HttpDispatcherLifecycleTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort port = new DynamicPort("httpPort");

    @Override
    protected String getConfigFile()
    {
        return "http-dispatcher-lifecycle-config.xml";
    }

    @Test
    public void dispatcherThreadFinishesAfterDispose() throws Exception
    {
        MuleClient client = muleContext.getClient();

        MuleMessage response = client.send("http://localhost:" + port.getValue(), TEST_MESSAGE, null);
        assertThat(response.getPayloadAsString(), equalTo(TEST_MESSAGE));

        muleContext.dispose();

        for (Thread thread : Thread.getAllStackTraces().keySet())
        {
            assertFalse(thread.getName().startsWith("http.request.dispatch"));
        }
    }
}
