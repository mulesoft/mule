/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.routing.outbound;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * Make sure to run an external amq broker, otherwise the test isn't possible.
 */
public class MulticastWithXaTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/routing/outbound/multicasting-router-xa-config.xml";
    }

    @Test
    public void testName() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage msg = new DefaultMuleMessage("Hi", client.getMuleContext());
        client.dispatch("jms://Myflow.input?connector=simpleJmsConnector", msg);
        MuleMessage result = client.request("jms://Myflow.finishedOriginal?connector=simpleJmsConnector", 10000);
        assertNotNull(result);
    }
}
