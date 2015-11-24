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
                "org/mule/test/integration/notifications/message-processor-notification-test-flow.xml"}});
    }

    @Override
    public void doTest() throws Exception
    {
        List<String> testList = Arrays.asList("test", "with", "collection");

        MuleClient client = muleContext.getClient();
        assertNotNull(client.send("vm://in-single", "test", null));
        assertNotNull(client.send("vm://in-processorChain", "test", null));
        assertNotNull(client.send("vm://customProcessor", "test", null));
        assertNotNull(client.send("vm://in-choice", "test", null));
        assertNotNull(client.send("vm://in-scatterGather", "test", null));

        assertNotNull(client.send("vm://in-foreach", "test", null));
        assertNotNull(client.send("vm://in-enricher", "test", null));
        //assertNotNull(client.send("vm://in-async", "test", null));
        assertNotNull(client.send("vm://in-filter", "test", null));
        assertNotNull(client.send("vm://idem-msg-filter", "test", null));
        assertNotNull(client.send("vm://idem-sh-msg-filter", "test", null));
        assertNotNull(client.send("vm://in-subflow", "test", null));
        assertNotNull(client.send("vm://in-catch", "test", null));
        assertNotNull(client.send("vm://in-rollback", "test", null));
        assertNotNull(client.send("vm://in-choice-es", "test", null));
        assertNotNull(client.send("vm://request-reply", "test", null));
        assertNotNull(client.send("vm://cs1", "test", null));
        assertNotNull(client.send("vm://cs2", "test", null));
        assertNotNull(client.send("vm://cs3", "test", null));
        assertNotNull(client.send("vm://cs4", "test" , null));
        assertNotNull(client.send("vm://fsucc", "test", null));
        assertNotNull(client.send("vm://round-robin", "test", null));
        assertNotNull(client.send("vm://recipient-list", "recipient", null));
        assertNotNull(client.send("vm://collection-agg", testList, null));
        assertNotNull(client.send("vm://custom-agg", testList, null));
        assertNotNull(client.send("vm://chunk-agg", "test", null));
        assertNotNull(client.send("vm://wire-tap", "test", null));
    }

    @Override
    public RestrictedNode getSpecification()
    {
        return new Node()
                //singleMP
                .serial(prePost())

                //processorChain
                .serial(pre()) //Message Processor Chain
                .serial(prePost()) //logger-1
                .serial(prePost()) //logger-2
                .serial(post()) //Message Processor Chain

                //custom-processor
                .serial(prePost())
                .serial(prePost())

                //choice
                .serial(pre()) //choice
                .serial(prePost())    //otherwise-logger
                .serial(post())

                // scatter-gather
                .serial(pre()) // scatter-gather
                .serial(new Node()
                    .parallel(pre() // route 1 chain
                        .serial(prePost()) // route 1 first logger
                        .serial(prePost()) // route 1 second logger
                        .serial(post())) // route 1 chain
                    .parallel(prePost())) // route 0 logger
                .serial(post()) // scatter-gather

                //foreach
                .serial(pre()) //foreach
                .serial(prePost())    //logger-loop-1
                .serial(prePost())    //logger-loop-2
                .serial(post())
                .serial(prePost())    //MP after the Scope

                //enricher
                .serial(pre()) //append-string
                .serial(prePost())
                .serial(post())
                .serial(pre()) //chain
                .serial(prePost())
                .serial(prePost())
                .serial(post())

                ////async             //This is unstable
                //.serial(prePost())
                //.serial(prePost())
                //.serial(prePost())

                //filter
                .serial(pre())
                .serial(prePost())
                .serial(post())

                //idempotent-message-filter
                .serial(pre())          //open message filter
                .serial(prePost())      //message processor
                .serial(post())         //close mf

                //idempotent-secure-hash-message-filter
                .serial(pre())          //open message filter
                .serial(prePost())      //message processor
                .serial(post())         //close mf

                //subflow
                .serial(prePost())
                .serial(pre())
                .serial(pre())
                .serial(prePost())
                .serial(post())
                .serial(post())

                //catch-es
                .serial(prePost())
                .serial(prePost())

                //rollback-es
                .serial(prePost())
                .serial(prePost())

                //choice-es
                .serial(prePost())
                .serial(prePost())

                //request-reply
                .serial(pre())
                .serial(prePost())
                .serial(post())
                .serial(prePost())

                //composite-source
                .serial(prePost()) //call throw cs1
                .serial(prePost())
                .serial(prePost())
                .serial(prePost()) //call throw cs4

                //first-successful
                .serial(prePost())
                .serial(prePost())

                //round-robin
                .serial(prePost())
                .serial(prePost())

                //recipient-list
                .serial(prePost())  //send message to the requested endpoint
                .serial(prePost())  //log message

                //collection-aggregator
                .serial(pre())      //open Splitter, unpacks three messages
                .serial(prePost())  //1st message on Logger
                .serial(prePost())  //gets to Aggregator
                .serial(prePost())  //2nd message on Logger
                .serial(prePost())  //gets to Aggregator
                .serial(prePost())  //3rd message on Logger
                .serial(prePost())  //gets to Aggregator and packs the three messages, then close
                .serial(post())     //close Splitter

                //custom-aggregator
                .serial(pre())      //open Splitter, unpacks three messages
                .serial(prePost())  //1st message, open Aggregator
                .serial(prePost())  //2nd message
                .serial(pre())      //3rd message, packs the three messages
                .serial(prePost())  //Logger process packed message
                .serial(post())     //close Aggregator
                .serial(post())     //close Splitter

                //chunk-aggregator
                .serial(pre())      //start Splitter
                .serial(prePost())  //1st message on Logger
                .serial(prePost())  //gets to Aggregator
                .serial(prePost())  //2nd message on Logger
                .serial(prePost())  //gets to Aggregator
                .serial(prePost())  //3rd message on Logger
                .serial(prePost())  //gets to Aggregator
                .serial(prePost())  //4th message on Logger
                .serial(pre())      //gets to Aggregator and packs four messages
                .serial(prePost())  //packed message get to the second Logger
                .serial(post())     //close Aggregator
                .serial(post())     //close Splitter

                //wire-tap
                .serial(prePost())
                .serial(prePost())

                ;
    }

    @Override
    public void validateSpecification(RestrictedNode spec) throws Exception
    {
    }
}
