/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing.outbound;

import static org.junit.Assert.assertNotNull;

import org.junit.Ignore;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.functional.junit4.FunctionalTestCase;

import org.junit.Test;

/**
 * Make sure to run an external amq broker, otherwise the test isn't possible.
 */
@Ignore("test for MULE-5515")
public class MulticastWithXaTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/routing/outbound/multicasting-router-xa-config.xml";
    }

    @Test
    public void testName() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage msg = new DefaultMuleMessage("Hi", muleContext);
        client.dispatch("jms://Myflow.input?connector=simpleJmsConnector", msg);
        MuleMessage result = client.request("jms://Myflow.finishedOriginal?connector=simpleJmsConnector", 10000);
        assertNotNull(result);
    }
}
