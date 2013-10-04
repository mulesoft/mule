/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transport.jdbc;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleEventContext;
import org.mule.api.context.notification.TransactionNotificationListener;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.routing.OutboundRouterCollection;
import org.mule.api.service.Service;
import org.mule.api.source.CompositeMessageSource;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transaction.TransactionFactory;
import org.mule.component.DefaultJavaComponent;
import org.mule.context.notification.TransactionNotification;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.exception.DefaultMessagingExceptionStrategy;
import org.mule.model.seda.SedaService;
import org.mule.object.PrototypeObjectFactory;
import org.mule.routing.outbound.DefaultOutboundRouterCollection;
import org.mule.routing.outbound.OutboundPassThroughRouter;
import org.mule.service.AbstractService;
import org.mule.tck.functional.EventCallback;
import org.mule.transaction.MuleTransactionConfig;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

public abstract class AbstractJdbcTransactionalFunctionalTestCase extends AbstractJdbcFunctionalTestCase  implements TransactionNotificationListener<TransactionNotification>
{

    private Transaction currentTx;
    protected boolean rollbacked = false;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        muleContext.registerListener(this);
        currentTx = null;
    }

    @Test
    public void testReceiveAndSendWithException() throws Exception
    {
        final AtomicBoolean called = new AtomicBoolean(false);

        EventCallback callback = new EventCallback()
        {
            public void eventReceived(MuleEventContext context, Object component) throws Exception
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
        initialiseService(TransactionConfig.ACTION_ALWAYS_BEGIN, callback);
        muleContext.start();

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

    public Service initialiseService(byte txBeginAction, EventCallback callback) throws Exception
    {
        Service service = new SedaService(muleContext);
        ((AbstractService) service).setExceptionListener(new DefaultMessagingExceptionStrategy(muleContext));
        service.setName("testComponent");
        service.setComponent(new DefaultJavaComponent(new PrototypeObjectFactory(JdbcFunctionalTestComponent.class)));

        TransactionFactory tf = getTransactionFactory();
        TransactionConfig txConfig = new MuleTransactionConfig(txBeginAction);
        txConfig.setFactory(tf);
        
        EndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(getInDest(), muleContext);
        endpointBuilder.setName("testIn");
        endpointBuilder.setConnector(connector);
        endpointBuilder.setTransactionConfig(txConfig);
        InboundEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint(
            endpointBuilder);

        EndpointBuilder endpointBuilder2 = new EndpointURIEndpointBuilder(getOutDest(), muleContext);
        endpointBuilder2.setName("testOut");
        endpointBuilder2.setConnector(connector);
        OutboundEndpoint outProvider = muleContext.getEndpointFactory().getOutboundEndpoint(
            endpointBuilder2);
        
        service.setOutboundMessageProcessor(new DefaultOutboundRouterCollection());
        OutboundPassThroughRouter router = new OutboundPassThroughRouter();
        router.addRoute(outProvider);
        ((OutboundRouterCollection) service.getOutboundMessageProcessor()).addRoute(router);
        ((CompositeMessageSource) service.getMessageSource()).addSource(endpoint);

        // these tests no longer work - they need replacing with config driven tests
        // furthemore, nothing is read from service properties any more
        // (except for axis and cxf related hacks)
        // so i am removing the code below since it's a pointless call to a deprecated method
//        HashMap props = new HashMap();
//        props.put("eventCallback", callback);
//        service.setProperties(props);
        service.setModel(model);
        muleContext.getRegistry().registerService(service);
        return service;
    }

    public void onNotification(TransactionNotification notification)
    {
        if (notification.getAction() == TransactionNotification.TRANSACTION_ROLLEDBACK)
        {
            this.rollbacked = true;
        }
    }

    abstract protected TransactionFactory getTransactionFactory();

}
