/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.client;

import org.mule.config.MuleProperties;
import org.mule.extras.client.MuleClient;
import org.mule.impl.MuleTransactionConfig;
import org.mule.impl.endpoint.EndpointURIEndpointBuilder;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.providers.jms.JmsTransactionFactory;
import org.mule.tck.FunctionalTestCase;
import org.mule.transaction.TransactionCallback;
import org.mule.transaction.TransactionCoordination;
import org.mule.transaction.TransactionTemplate;
import org.mule.transformers.simple.ByteArrayToString;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOTransaction;
import org.mule.umo.UMOTransactionConfig;
import org.mule.umo.endpoint.UMOEndpointBuilder;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MuleClientTransactionTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/test/integration/client/test-client-jms-mule-config.xml";
    }

    public void testTransactionsWithSetRollbackOnly() throws Exception
    {
        final MuleClient client = new MuleClient();
        final Map props = new HashMap();
        props.put("JMSReplyTo", "replyTo.queue");
        props.put(MuleProperties.MULE_REMOTE_SYNC_PROPERTY, "false");

        // Empty reply queue
        while (client.receive("jms://replyTo.queue", 2000) != null)
        {
            // slurp
        }

        MuleTransactionConfig tc = new MuleTransactionConfig();
        tc.setFactory(new JmsTransactionFactory());
        tc.setAction(UMOTransactionConfig.ACTION_ALWAYS_BEGIN);

        // This enpoint needs to be registered prior to use cause we need to set
        // the transaction config so that the endpoint will "know" it is transacted
        // and not close the session itself but leave it up to the transaction.
        UMOEndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder("jms://test.queue", managementContext);
        endpointBuilder.setTransactionConfig(tc);
        endpointBuilder.setName("TransactedTest.Queue");
        UMOImmutableEndpoint inboundEndpoint = managementContext.getRegistry()
            .lookupEndpointFactory()
            .getOutboundEndpoint(endpointBuilder, managementContext);
        client.getManagementContext().getRegistry().registerEndpoint(inboundEndpoint);


        TransactionTemplate tt = new TransactionTemplate(tc, null, managementContext);
        tt.execute(new TransactionCallback()
        {
            public Object doInTransaction() throws Exception
            {
                for (int i = 0; i < 100; i++)
                {
                    client.send("TransactedTest.Queue", "Test Client Dispatch message " + i, props);
                }
                UMOTransaction tx = TransactionCoordination.getInstance().getTransaction();
                assertNotNull(tx);
                tx.setRollbackOnly();
                return null;
            }
        });

        UMOMessage result = client.receive("jms://replyTo.queue", 2000);
        assertNull(result);
    }

    public void testTransactionsWithExceptionThrown() throws Exception
    {
        final MuleClient client = new MuleClient();
        final Map props = new HashMap();
        props.put("JMSReplyTo", "replyTo.queue");
        props.put(MuleProperties.MULE_REMOTE_SYNC_PROPERTY, "false");

        // Empty reply queue
        while (client.receive("jms://replyTo.queue", 2000) != null)
        {
            // hmm..mesages
        }

        MuleTransactionConfig tc = new MuleTransactionConfig();
        tc.setFactory(new JmsTransactionFactory());
        tc.setAction(UMOTransactionConfig.ACTION_ALWAYS_BEGIN);

        // This enpoint needs to be registered prior to use cause we need to set
        // the transaction config so that the endpoint will "know" it is transacted
        // and not close the session itself but leave it up to the transaction.
        UMOEndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder("jms://test.queue", managementContext);
        endpointBuilder.setTransactionConfig(tc);
        endpointBuilder.setName("TransactedTest.Queue");
        UMOImmutableEndpoint inboundEndpoint = managementContext.getRegistry()
            .lookupEndpointFactory()
            .getOutboundEndpoint(endpointBuilder, managementContext);
        client.getManagementContext().getRegistry().registerEndpoint(inboundEndpoint);

        TransactionTemplate tt = new TransactionTemplate(tc, null, managementContext);
        try
        {
            tt.execute(new TransactionCallback()
            {
                public Object doInTransaction() throws Exception
                {
                    for (int i = 0; i < 100; i++)
                    {
                        client.send("TransactedTest.Queue", "Test Client Dispatch message " + i, props);
                    }
                    throw new Exception();
                }
            });
            fail();
        }
        catch (Exception e)
        {
            // this is ok
        }

        UMOMessage result = client.receive("jms://replyTo.queue", 2000);
        assertNull(result);
    }

    public void testTransactionsWithCommit() throws Exception
    {
        final MuleClient client = new MuleClient();
        final Map props = new HashMap();
        props.put("JMSReplyTo", "replyTo.queue");
        props.put(MuleProperties.MULE_REMOTE_SYNC_PROPERTY, "false");
        props.put("transacted", "true");

        // Empty reply queue
        while (client.receive("jms://replyTo.queue", 2000) != null)
        {
            // yum!
        }

        MuleTransactionConfig tc = new MuleTransactionConfig();
        tc.setFactory(new JmsTransactionFactory());
        tc.setAction(UMOTransactionConfig.ACTION_ALWAYS_BEGIN);

        // This enpoint needs to be registered prior to use cause we need to set
        // the transaction config so that the endpoint will "know" it is transacted
        // and not close the session itself but leave it up to the transaction.
        UMOEndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder("jms://test.queue", managementContext);
        endpointBuilder.setTransactionConfig(tc);
        endpointBuilder.setName("TransactedTest.Queue");
        UMOImmutableEndpoint inboundEndpoint = managementContext.getRegistry()
            .lookupEndpointFactory()
            .getOutboundEndpoint(endpointBuilder, managementContext);
        client.getManagementContext().getRegistry().registerEndpoint(inboundEndpoint);


        TransactionTemplate tt = new TransactionTemplate(tc, null, managementContext);
        tt.execute(new TransactionCallback()
        {
            public Object doInTransaction() throws Exception
            {
                for (int i = 0; i < 100; i++)
                {
                    client.send("TransactedTest.Queue", "Test Client Dispatch message " + i, props);
                }
                return null;
            }
        });

        for (int i = 0; i < 100; i++)
        {
            UMOMessage result = client.receive("jms://replyTo.queue", 2000);
            assertNotNull(result);
        }
        UMOMessage result = client.receive("jms://replyTo.queue", 2000);
        assertNull(result);
    }

    protected void emptyReplyQueue() throws Exception
    {
        final MuleClient client = new MuleClient();
        MuleTransactionConfig tc = new MuleTransactionConfig();
        tc.setFactory(new JmsTransactionFactory());
        tc.setAction(UMOTransactionConfig.ACTION_ALWAYS_BEGIN);
        TransactionTemplate tt = new TransactionTemplate(tc, null, managementContext);
        tt.execute(new TransactionCallback()
        {
            public Object doInTransaction() throws Exception
            {
                while (client.receive("jms://replyTo.queue", 2000) != null)
                {
                    // munch..
                }

                return null;
            }
        });
    }

}
