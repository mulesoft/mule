/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.mule.DefaultMuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.construct.Flow;
import org.mule.context.notification.ClusterNodeNotification;
import org.mule.source.ClusterizableMessageSourceWrapper;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.PollingController;

import org.junit.Test;

public class ClusterizableMessageSourceFlowTestCase extends FunctionalTestCase
{

    public ClusterizableMessageSourceFlowTestCase()
    {
        setStartContext(false);
    }

    @Override
    protected String getConfigResources()
    {
        return "clusterizable-message-source-flow-config.xml";
    }

    @Test
    public void startsWhenPrimaryNode() throws Exception
    {
        muleContext.start();

        LocalMuleClient client = muleContext.getClient();
        MuleMessage response = client.request("vm://testOut", 5000);
        assertEquals("TEST", response.getPayload());
    }

    @Test
    public void doesNotStartsWhenSecondaryNode() throws Exception
    {
        TestPollingController pollingController = new TestPollingController();
        ((DefaultMuleContext) muleContext).setPollingController(pollingController);
        muleContext.start();

        Flow test1 = (Flow) muleContext.getRegistry().get("test1");
        ClusterizableMessageSourceWrapper messageSource = (ClusterizableMessageSourceWrapper) test1.getMessageSource();
        assertTrue(test1.isStarted());
        assertTrue(messageSource.isStarted());
    }

    @Test
    public void startsWhenNodeBecomePrimary() throws Exception
    {
        TestPollingController pollingController = new TestPollingController();
        ((DefaultMuleContext) muleContext).setPollingController(pollingController);

        muleContext.start();

        Flow test1 = (Flow) muleContext.getRegistry().get("test1");
        ClusterizableMessageSourceWrapper messageSource = (ClusterizableMessageSourceWrapper) test1.getMessageSource();

        messageSource.initialise();

        pollingController.isPrimary=true;
        muleContext.fireNotification(new ClusterNodeNotification("primary", ClusterNodeNotification.PRIMARY_CLUSTER_NODE_SELECTED));

        LocalMuleClient client = muleContext.getClient();
        MuleMessage response = client.request("vm://testOut", 5000);
        assertEquals("TEST", response.getPayload());
    }

    private class TestPollingController implements PollingController
    {

        boolean isPrimary;

        @Override
        public boolean isPrimaryPollingInstance()
        {
            return isPrimary;
        }
    }
}
