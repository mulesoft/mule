/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context.notification.processors;

import static org.junit.Assert.assertNotNull;

import org.mule.api.client.MuleClient;
import org.mule.context.notification.Node;
import org.mule.context.notification.RestrictedNode;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections.Factory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.Parameterized;

public class MessageProcessorNotificationTestCase extends AbstractMessageProcessorNotificationTestCase
{

    public MessageProcessorNotificationTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {{ConfigVariant.FLOW,
                                              "org/mule/test/integration/notifications/message-processor-notification-test-flow.xml"}
        });
    }

    private MuleClient client;

    @Before
    public void before()
    {
        client = muleContext.getClient();
    }

    private Factory specificationFactory;

    @Test
    public void single() throws Exception
    {
        specificationFactory = new Factory()
        {

            @Override
            public Object create()
            {
                return new Node()
                                 .serial(prePost());
            }
        };

        assertNotNull(client.send("vm://in-single", "test", null));

        assertNotifications();
    }

    @Test
    public void chain() throws Exception
    {
        specificationFactory = new Factory()
        {

            @Override
            public Object create()
            {
                return new Node()
                                 .serial(pre()) // Message Processor Chain
                                 .serial(prePost()) // logger-1
                                 .serial(prePost()) // logger-2
                                 .serial(post()) // Message Processor Chain
                ;
            }
        };

        assertNotNull(client.send("vm://in-processorChain", "test", null));

        assertNotifications();
    }

    @Test
    public void customProcessor() throws Exception
    {
        specificationFactory = new Factory()
        {

            @Override
            public Object create()
            {
                return new Node()
                                 .serial(prePost())
                                 .serial(prePost());
            }
        };

        assertNotNull(client.send("vm://customProcessor", "test", null));

        assertNotifications();
    }

    @Test
    public void choice() throws Exception
    {
        specificationFactory = new Factory()
        {

            @Override
            public Object create()
            {
                return new Node()
                                 // choice
                                 .serial(pre()) // choice
                                 .serial(prePost()) // otherwise-logger
                                 .serial(post());
            }
        };

        assertNotNull(client.send("vm://in-choice", "test", null));

        assertNotifications();
    }

    @Test
    public void foreach() throws Exception
    {
        specificationFactory = new Factory()
        {

            @Override
            public Object create()
            {
                return new Node()
                                 .serial(pre()) // foreach
                                 .serial(prePost()) // logger-loop-1
                                 .serial(prePost()) // logger-loop-2
                                 .serial(post())
                                 .serial(prePost()) // MP after the Scope
                ;
            }
        };

        assertNotNull(client.send("vm://in-foreach", "test", null));

        assertNotifications();
    }

    @Test
    public void enricher() throws Exception
    {
        specificationFactory = new Factory()
        {

            @Override
            public Object create()
            {
                return new Node()
                                 .serial(pre()) // append-string
                                 .serial(prePost())
                                 .serial(post())
                                 .serial(pre()) // chain
                                 .serial(prePost())
                                 .serial(prePost())
                                 .serial(post());
            }
        };

        assertNotNull(client.send("vm://in-enricher", "test", null));

        assertNotifications();
    }

    @Test
    @Ignore("This is unstable")
    public void async() throws Exception
    {
        specificationFactory = new Factory()
        {

            @Override
            public Object create()
            {
                return new Node()
                                 .serial(prePost())
                                 .serial(prePost())
                                 .serial(prePost());
            }
        };

        assertNotNull(client.send("vm://in-async", "test", null));

        assertNotifications();
    }

    @Test
    public void filter() throws Exception
    {
        specificationFactory = new Factory()
        {

            @Override
            public Object create()
            {
                return new Node()
                                 .serial(pre())
                                 .serial(prePost())
                                 .serial(post());
            }
        };

        assertNotNull(client.send("vm://in-filter", "test", null));

        assertNotifications();
    }

    @Test
    public void idempotentMessageFilter() throws Exception
    {
        specificationFactory = new Factory()
        {

            @Override
            public Object create()
            {
                return new Node()
                                 .serial(pre()) // open message filter
                                 .serial(prePost()) // message processor
                                 .serial(post()) // close mf
                ;
            }
        };

        assertNotNull(client.send("vm://idem-msg-filter", "test", null));

        assertNotifications();
    }

    @Test
    public void idempotentSecureHashMessageFilter() throws Exception
    {
        specificationFactory = new Factory()
        {

            @Override
            public Object create()
            {
                return new Node()
                                 .serial(pre()) // open message filter
                                 .serial(prePost()) // message processor
                                 .serial(post()) // close mf
                ;
            }
        };

        assertNotNull(client.send("vm://idem-sh-msg-filter", "test", null));

        assertNotifications();
    }

    @Test
    public void subFlow() throws Exception
    {
        specificationFactory = new Factory()
        {

            @Override
            public Object create()
            {
                return new Node()
                                 .serial(prePost())
                                 .serial(pre())
                                 .serial(pre())
                                 .serial(prePost())
                                 .serial(post())
                                 .serial(post());
            }
        };

        assertNotNull(client.send("vm://in-subflow", "test", null));

        assertNotifications();
    }

    @Test
    public void catchExceptionStrategy() throws Exception
    {
        specificationFactory = new Factory()
        {

            @Override
            public Object create()
            {
                return new Node()
                                 // catch-es
                                 .serial(prePost())
                                 .serial(prePost());
            }
        };

        assertNotNull(client.send("vm://in-catch", "test", null));

        assertNotifications();
    }

    @Test
    public void rollbackExceptionStrategy() throws Exception
    {
        specificationFactory = new Factory()
        {

            @Override
            public Object create()
            {
                return new Node()
                                 // rollback-es
                                 .serial(prePost())
                                 .serial(prePost());
            }
        };

        assertNotNull(client.send("vm://in-rollback", "test", null));

        assertNotifications();
    }

    @Test
    public void choiceExceptionStrategy() throws Exception
    {
        specificationFactory = new Factory()
        {

            @Override
            public Object create()
            {
                return new Node()
                                 .serial(prePost())
                                 .serial(prePost());
            }
        };

        assertNotNull(client.send("vm://in-choice-es", "test", null));

        assertNotifications();
    }

    @Test
    public void requestReply() throws Exception
    {
        specificationFactory = new Factory()
        {

            @Override
            public Object create()
            {
                return new Node()
                                 .serial(pre())
                                 .serial(prePost())
                                 .serial(post())
                                 .serial(prePost());
            }
        };

        assertNotNull(client.send("vm://request-reply", "test", null));

        assertNotifications();
    }

    @Test
    public void compositeSource() throws Exception
    {
        specificationFactory = new Factory()
        {

            @Override
            public Object create()
            {
                return new Node()
                                 .serial(prePost()) // call throw cs1
                                 .serial(prePost())
                                 .serial(prePost())
                                 .serial(prePost()) // call throw cs4
                ;
            }
        };

        assertNotNull(client.send("vm://cs1", "test", null));
        assertNotNull(client.send("vm://cs2", "test", null));
        assertNotNull(client.send("vm://cs3", "test", null));
        assertNotNull(client.send("vm://cs4", "test", null));

        assertNotifications();
    }

    @Test
    public void firstSuccessful() throws Exception
    {
        specificationFactory = new Factory()
        {

            @Override
            public Object create()
            {
                return new Node()
                                 .serial(prePost()) // logger
                                 .serial(pre()) // first-successful
                                 .serial(prePost())
                                 .serial(prePost())
                                 .serial(prePost())
                                 .serial(prePost()) // dlq
                                 .serial(post());
            }
        };

        assertNotNull(client.send("vm://fsucc", "test", null));

        assertNotifications();
    }

    @Test
    public void roundRobin() throws Exception
    {
        specificationFactory = new Factory()
        {

            @Override
            public Object create()
            {
                return new Node()
                                 .serial(pre()) // round-robin
                                 .serial(prePost()) // inner logger
                                 .serial(post())
                                 .serial(prePost()) // logger
                ;
            }
        };

        assertNotNull(client.send("vm://round-robin", "test", null));

        assertNotifications();
    }

    @Test
    public void recipientList() throws Exception
    {
        specificationFactory = new Factory()
        {

            @Override
            public Object create()
            {
                return new Node()
                                 .serial(prePost()) // send message to the requested endpoint
                                 .serial(prePost()) // log message
                ;
            }
        };

        assertNotNull(client.send("vm://recipient-list", "recipient", null));

        assertNotifications();
    }

    @Test
    public void collectionAggregator() throws Exception
    {
        specificationFactory = new Factory()
        {
            @Override
            public Object create()
            {
                return new Node()
                                 .serial(pre()) // open Splitter, unpacks three messages
                                 .serial(prePost()) // 1st message on Logger
                                 .serial(prePost()) // gets to Aggregator
                                 .serial(prePost()) // 2nd message on Logger
                                 .serial(prePost()) // gets to Aggregator
                                 .serial(prePost()) // 3rd message on Logger
                                 .serial(prePost()) // gets to Aggregator and packs the three messages, then close
                                 .serial(post()) // close Splitter
                ;
            }
        };

        List<String> testList = Arrays.asList("test", "with", "collection");
        assertNotNull(client.send("vm://collection-agg", testList, null));

        assertNotifications();
    }

    @Test
    public void customAggregator() throws Exception
    {
        specificationFactory = new Factory()
        {
            @Override
            public Object create()
            {
                return new Node()
                                 .serial(pre()) // open Splitter, unpacks three messages
                                 .serial(prePost()) // 1st message, open Aggregator
                                 .serial(prePost()) // 2nd message
                                 .serial(pre()) // 3rd message, packs the three messages
                                 .serial(prePost()) // Logger process packed message
                                 .serial(post()) // close Aggregator
                                 .serial(post()) // close Splitter
                ;
            }
        };

        List<String> testList = Arrays.asList("test", "with", "collection");
        assertNotNull(client.send("vm://custom-agg", testList, null));

        assertNotifications();
    }

    @Test
    public void chunkAggregator() throws Exception
    {
        specificationFactory = new Factory()
        {

            @Override
            public Object create()
            {
                return new Node()
                                 .serial(pre()) // start Splitter
                                 .serial(prePost()) // 1st message on Logger
                                 .serial(prePost()) // gets to Aggregator
                                 .serial(prePost()) // 2nd message on Logger
                                 .serial(prePost()) // gets to Aggregator
                                 .serial(prePost()) // 3rd message on Logger
                                 .serial(prePost()) // gets to Aggregator
                                 .serial(prePost()) // 4th message on Logger
                                 .serial(pre()) // gets to Aggregator and packs four messages
                                 .serial(prePost()) // packed message get to the second Logger
                                 .serial(post()) // close Aggregator
                                 .serial(post()) // close Splitter
                ;
            }
        };

        assertNotNull(client.send("vm://chunk-agg", "test", null));

        assertNotifications();
    }

    @Test
    public void wireTap() throws Exception
    {
        specificationFactory = new Factory()
        {

            @Override
            public Object create()
            {
                return new Node()
                                 .serial(prePost())
                                 .serial(prePost());
            }
        };

        assertNotNull(client.send("vm://wire-tap", "test", null));

        assertNotifications();
    }

    @Test
    public void untilSuccesful() throws Exception
    {
        specificationFactory = new Factory()
        {

            @Override
            public Object create()
            {
                return new Node()
                                 .serial(pre())
                                 .serial(new Node()
                                                   .parallelSynch(prePost())
                                                   .parallelSynch(post().serial(prePost())));
            }
        };

        assertNotNull(client.send("vm://until-successful", "test", null));
        client.request("vm://out-us", RECEIVE_TIMEOUT);

        assertNotifications();
    }

    @Test
    public void untilSuccesfulWithProcessorChain() throws Exception
    {
        specificationFactory = new Factory()
        {

            @Override
            public Object create()
            {
                return new Node()
                                 .serial(pre())
                                 .serial(new Node()
                                                   .parallelSynch(pre().serial(prePost()).serial(prePost()).serial(post()))
                                                   .parallelSynch(post().serial(prePost())));
            }
        };

        assertNotNull(client.send("vm://until-successful-with-processor-chain", "test", null));
        client.request("vm://out-us", RECEIVE_TIMEOUT);

        assertNotifications();
    }

    @Test
    public void untilSuccesfulWithEnricher() throws Exception
    {
        specificationFactory = new Factory()
        {

            @Override
            public Object create()
            {
                return new Node()
                                 .serial(pre())
                                 .serial(new Node()
                                                   .parallelSynch(pre().serial(prePost()).serial(post()))
                                                   .parallelSynch(post().serial(prePost())));
            }
        };

        assertNotNull(client.send("vm://until-successful-with-enricher", "test", null));

        assertNotifications();
    }

    @Override
    public RestrictedNode getSpecification()
    {
        return (RestrictedNode) specificationFactory.create();
    }

    @Override
    public void validateSpecification(RestrictedNode spec) throws Exception
    {
    }
}
