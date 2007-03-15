/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.providers.jdbc;


import org.mule.impl.DefaultExceptionStrategy;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.MuleTransactionConfig;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.routing.inbound.InboundRouterCollection;
import org.mule.routing.outbound.OutboundPassThroughRouter;
import org.mule.routing.outbound.OutboundRouterCollection;
import org.mule.tck.functional.EventCallback;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOTransaction;
import org.mule.umo.UMOTransactionConfig;
import org.mule.umo.UMOTransactionFactory;
import org.mule.umo.endpoint.UMOEndpoint;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;

import java.util.HashMap;

/**
 * @author Guillaume Nodet
 * @version $Revision$
 */
public abstract class AbstractJdbcTransactionalFunctionalTestCase extends AbstractJdbcFunctionalTestCase
{

    private UMOTransaction currentTx;

    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        currentTx = null;
    }

    public void testReceiveAndSendWithException() throws Exception
    {
        final AtomicBoolean called = new AtomicBoolean(false);

        EventCallback callback = new EventCallback()
        {
            public void eventReceived(UMOEventContext context, Object Component) throws Exception
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

        execSqlUpdate("INSERT INTO TEST(ID, TYPE, DATA, ACK, RESULT) VALUES (NULL, 1, '" + DEFAULT_MESSAGE
                      + "', NULL, NULL)");

        synchronized (called)
        {
            called.wait(20000);
        }
        assertTrue(called.get());

        Thread.sleep(1000);

        assertNotNull(currentTx);
        assertTrue(currentTx.isRolledBack());

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

        UMODescriptor descriptor = new MuleDescriptor();
        descriptor.setExceptionListener(new DefaultExceptionStrategy());
        descriptor.setName("testComponent");
        descriptor.setImplementation(JdbcFunctionalTestComponent.class.getName());

        UMOEndpoint endpoint = new MuleEndpoint("testIn", getInDest(), connector, null,
            UMOEndpoint.ENDPOINT_TYPE_RECEIVER, 0, null, null);

        UMOTransactionFactory tf = getTransactionFactory();
        UMOTransactionConfig txConfig = new MuleTransactionConfig();
        txConfig.setFactory(tf);
        txConfig.setAction(txBeginAction);

        UMOEndpoint outProvider = new MuleEndpoint("testOut", getOutDest(), connector, null,
            UMOEndpoint.ENDPOINT_TYPE_SENDER, 0, null, null);

        endpoint.setTransactionConfig(txConfig);

        descriptor.setOutboundRouter(new OutboundRouterCollection());
        OutboundPassThroughRouter router = new OutboundPassThroughRouter();
        router.addEndpoint(outProvider);
        descriptor.getOutboundRouter().addRouter(router);
        descriptor.setInboundRouter(new InboundRouterCollection());
        descriptor.getInboundRouter().addEndpoint(endpoint);
        
        HashMap props = new HashMap();
        props.put("eventCallback", callback);
        descriptor.setProperties(props);
        UMOComponent component = model.registerComponent(descriptor);
        return component;
    }

    abstract protected UMOTransactionFactory getTransactionFactory();

}
