/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms.issues;

import org.mule.api.MuleEventContext;
import org.mule.component.DefaultJavaComponent;
import org.mule.transport.jms.JmsConnector;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.transport.jms.integration.AbstractJmsFunctionalTestCase;
import org.mule.util.concurrent.Latch;

import javax.jms.JMSException;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

public class JmsReconnectionFunctionalMule1720TestCase extends AbstractJmsFunctionalTestCase
{
    private long RECONNECTION_TIMEOUT = 5000;
    private Latch componentCallbackLatch;

    protected String getConfigResources()
    {
        return "providers/activemq/jms-reconnection-strategy.xml";
    }
    
	public void testReconnectionAfterConnectionFailure() throws Exception {
	    componentCallbackLatch = new Latch();
	    EventCallback callback = new EventCallback()
        {
            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {
                componentCallbackLatch.countDown();
            }
        };
        DefaultJavaComponent defaultComponent = (DefaultJavaComponent) muleContext.getRegistry().lookupService("InputPart").getComponent();
        FunctionalTestComponent testComponent = (FunctionalTestComponent) defaultComponent.getObjectFactory().getInstance();
        testComponent.setEventCallback(callback);
        
        send(super.scenarioNoTx);
        assertTrue(componentCallbackLatch.await(LOCK_WAIT, TimeUnit.MILLISECONDS));
        
        componentCallbackLatch = new Latch();
        simulateConnectionFailure();
        Thread.sleep(RECONNECTION_TIMEOUT);

        send(super.scenarioNoTx);
        assertTrue(componentCallbackLatch.await(LOCK_WAIT, TimeUnit.MILLISECONDS));
	}
	
	private void simulateConnectionFailure() throws Exception
	{
        JmsConnector connector = (JmsConnector) muleContext.getRegistry().lookupConnector("jmsConnector");
        connector.getConnection().getExceptionListener().onException(new JMSException("fake disconnect!"));
	}
}
