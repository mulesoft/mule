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

import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionCallback;
import org.mule.api.transaction.TransactionConfig;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.endpoint.URIBuilder;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.transaction.MuleTransactionConfig;
import org.mule.transaction.TransactionCoordination;
import org.mule.transaction.TransactionTemplate;
import org.mule.transport.jms.JmsTransactionFactory;

import java.util.HashMap;
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
        while (client.request("jms://replyTo.queue", 2000) != null)
        {
            // slurp
        }

        MuleTransactionConfig tc = new MuleTransactionConfig();
        tc.setFactory(new JmsTransactionFactory());
        tc.setAction(TransactionConfig.ACTION_ALWAYS_BEGIN);

        // This enpoint needs to be registered prior to use cause we need to set
        // the transaction config so that the endpoint will "know" it is transacted
        // and not close the session itself but leave it up to the transaction.
        EndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(
            new URIBuilder("jms://test.queue"), muleContext);
        endpointBuilder.setTransactionConfig(tc);
        endpointBuilder.setName("TransactedTest.Queue");
        ImmutableEndpoint inboundEndpoint = muleContext.getRegistry()
                .lookupEndpointFactory()
                .getOutboundEndpoint(endpointBuilder);
        client.getMuleContext().getRegistry().registerEndpoint(inboundEndpoint);

        TransactionTemplate tt = new TransactionTemplate(tc, null, muleContext);
        tt.execute(new TransactionCallback()
        {
            public Object doInTransaction() throws Exception
            {
                for (int i = 0; i < 100; i++)
                {
                    client.send("jms://test.queue", "Test Client Dispatch message " + i, props);
                }
                Transaction tx = TransactionCoordination.getInstance().getTransaction();
                assertNotNull(tx);
                tx.setRollbackOnly();
                return null;
            }
        });

        MuleMessage result = client.request("jms://replyTo.queue", 2000);
        assertNull(result);
    }

    public void testTransactionsWithExceptionThrown() throws Exception
    {
        final MuleClient client = new MuleClient();
        final Map props = new HashMap();
        props.put("JMSReplyTo", "replyTo.queue");
        props.put(MuleProperties.MULE_REMOTE_SYNC_PROPERTY, "false");

        // Empty reply queue
        while (client.request("jms://replyTo.queue", 2000) != null)
        {
            // hmm..mesages
        }

        MuleTransactionConfig tc = new MuleTransactionConfig();
        tc.setFactory(new JmsTransactionFactory());
        tc.setAction(TransactionConfig.ACTION_ALWAYS_BEGIN);

        // This enpoint needs to be registered prior to use cause we need to set
        // the transaction config so that the endpoint will "know" it is transacted
        // and not close the session itself but leave it up to the transaction.
        EndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(
            new URIBuilder("jms://test.queue"), muleContext);
        endpointBuilder.setTransactionConfig(tc);
        endpointBuilder.setName("TransactedTest.Queue");
        ImmutableEndpoint inboundEndpoint = muleContext.getRegistry()
                .lookupEndpointFactory()
                .getOutboundEndpoint(endpointBuilder);
        client.getMuleContext().getRegistry().registerEndpoint(inboundEndpoint);

        TransactionTemplate tt = new TransactionTemplate(tc, null, muleContext);
        try
        {
            tt.execute(new TransactionCallback()
            {
                public Object doInTransaction() throws Exception
                {
                    for (int i = 0; i < 100; i++)
                    {
                        client.send("jms://test.queue", "Test Client Dispatch message " + i, props);
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

        MuleMessage result = client.request("jms://replyTo.queue", 2000);
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
        while (client.request("jms://replyTo.queue", 2000) != null)
        {
            // yum!
        }

        MuleTransactionConfig tc = new MuleTransactionConfig();
        tc.setFactory(new JmsTransactionFactory());
        tc.setAction(TransactionConfig.ACTION_ALWAYS_BEGIN);

        // This enpoint needs to be registered prior to use cause we need to set
        // the transaction config so that the endpoint will "know" it is transacted
        // and not close the session itself but leave it up to the transaction.
        EndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(
            new URIBuilder("jms://test.queue"), muleContext);
        endpointBuilder.setTransactionConfig(tc);
        endpointBuilder.setName("TransactedTest.Queue");
        ImmutableEndpoint inboundEndpoint = muleContext.getRegistry()
                .lookupEndpointFactory()
                .getOutboundEndpoint(endpointBuilder);
        client.getMuleContext().getRegistry().registerEndpoint(inboundEndpoint);


        TransactionTemplate tt = new TransactionTemplate(tc, null, muleContext);
        tt.execute(new TransactionCallback()
        {
            public Object doInTransaction() throws Exception
            {
                for (int i = 0; i < 100; i++)
                {
                    client.send("jms://test.queue", "Test Client Dispatch message " + i, props);
                }
                return null;
            }
        });

        for (int i = 0; i < 100; i++)
        {
            MuleMessage result = client.request("jms://replyTo.queue", 2000);
            assertNotNull(result);
        }
        MuleMessage result = client.request("jms://replyTo.queue", 2000);
        assertNull(result);
    }

    protected void emptyReplyQueue() throws Exception
    {
        final MuleClient client = new MuleClient();
        MuleTransactionConfig tc = new MuleTransactionConfig();
        tc.setFactory(new JmsTransactionFactory());
        tc.setAction(TransactionConfig.ACTION_ALWAYS_BEGIN);
        TransactionTemplate tt = new TransactionTemplate(tc, null, muleContext);
        tt.execute(new TransactionCallback()
        {
            public Object doInTransaction() throws Exception
            {
                while (client.request("jms://replyTo.queue", 2000) != null)
                {
                    // munch..
                }

                return null;
            }
        });
    }

}
