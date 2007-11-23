/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.providers.jdbc;


import org.mule.impl.DefaultExceptionStrategy;
import org.mule.impl.MuleTransactionConfig;
import org.mule.impl.endpoint.EndpointURIEndpointBuilder;
import org.mule.impl.internal.notifications.TransactionNotification;
import org.mule.impl.internal.notifications.TransactionNotificationListener;
import org.mule.impl.model.seda.SedaComponent;
import org.mule.routing.inbound.InboundRouterCollection;
import org.mule.routing.outbound.OutboundPassThroughRouter;
import org.mule.routing.outbound.OutboundRouterCollection;
import org.mule.tck.functional.EventCallback;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOTransaction;
import org.mule.umo.UMOTransactionConfig;
import org.mule.umo.UMOTransactionFactory;
import org.mule.umo.endpoint.UMOEndpointBuilder;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.manager.UMOServerNotification;
import org.mule.util.object.SimpleObjectFactory;

import java.util.HashMap;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractJdbcTransactionalFunctionalTestCase extends AbstractJdbcFunctionalTestCase  implements TransactionNotificationListener
{

    private UMOTransaction currentTx;
    protected boolean rollbacked = false;

    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        managementContext.registerListener(this);
        currentTx = null;
    }

    public void testReceiveAndSendWithException() throws Exception
    {
        final AtomicBoolean called = new AtomicBoolean(false);

        EventCallback callback = new EventCallback()
        {
            public void eventReceived(UMOEventContext context, Object component) throws Exception
            {
                try
                {
                    called.set(true);
                    currentTx = context.getCurrentTransaction();
                    assertNotNull(currentTx);
                    assertTrue(currentTx.isBegun());
                    currentTx.setRollbackOnly();
                }
                finally
                {
                    synchronized (called)
                    {
                        called.notifyAll();
                    }
                }
            }
        };

        // Start the server
        initialiseComponent(UMOTransactionConfig.ACTION_ALWAYS_BEGIN, callback);
        managementContext.start();

        execSqlUpdate("INSERT INTO TEST(TYPE, DATA, ACK, RESULT) VALUES (1, '" + DEFAULT_MESSAGE
                      + "', NULL, NULL)");

        synchronized (called)
        {
            called.wait(20000);
        }
        assertTrue(called.get());

        Thread.sleep(1000);

        assertTrue(rollbacked);

        Object[] obj = execSqlQuery("SELECT COUNT(*) FROM TEST WHERE TYPE = 2");
        assertNotNull(obj);
        assertEquals(1, obj.length);
        assertEquals(new Integer(0), obj[0]);
        obj = execSqlQuery("SELECT ACK FROM TEST WHERE TYPE = 1");
        assertNotNull(obj);
        assertEquals(1, obj.length);
        assertNull(obj[0]);
    }

    public UMOComponent initialiseComponent(byte txBeginAction, EventCallback callback) throws Exception
    {

        UMOComponent component = new SedaComponent();
        component.setExceptionListener(new DefaultExceptionStrategy());
        component.setName("testComponent");
        component.setServiceFactory(new SimpleObjectFactory(JdbcFunctionalTestComponent.class));

        UMOTransactionFactory tf = getTransactionFactory();
        UMOTransactionConfig txConfig = new MuleTransactionConfig();
        txConfig.setFactory(tf);
        txConfig.setAction(txBeginAction);
        
        UMOEndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(getInDest(), managementContext);
        endpointBuilder.setName("testIn");
        endpointBuilder.setConnector(connector);
        endpointBuilder.setTransactionConfig(txConfig);
        UMOImmutableEndpoint endpoint = managementContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(
            endpointBuilder);

        UMOEndpointBuilder endpointBuilder2 = new EndpointURIEndpointBuilder(getOutDest(), managementContext);
        endpointBuilder2.setName("testOut");
        endpointBuilder2.setConnector(connector);
        UMOImmutableEndpoint outProvider = managementContext.getRegistry().lookupEndpointFactory().getOutboundEndpoint(
            endpointBuilder2);
        
        component.setOutboundRouter(new OutboundRouterCollection());
        OutboundPassThroughRouter router = new OutboundPassThroughRouter();
        router.addEndpoint(outProvider);
        component.getOutboundRouter().addRouter(router);
        component.setInboundRouter(new InboundRouterCollection());
        component.getInboundRouter().addEndpoint(endpoint);

        HashMap props = new HashMap();
        props.put("eventCallback", callback);
        component.setProperties(props);
        component.setModel(model);
        managementContext.getRegistry().registerComponent(component);
        return component;
    }

    public void onNotification(UMOServerNotification notification)
    {
        if (notification.getAction() == TransactionNotification.TRANSACTION_ROLLEDBACK)
        {
            this.rollbacked = true;
        }
    }

    abstract protected UMOTransactionFactory getTransactionFactory();

}
