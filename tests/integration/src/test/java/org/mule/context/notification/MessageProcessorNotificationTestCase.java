/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context.notification;

import static org.junit.Assert.assertNotNull;

import org.mule.module.client.MuleClient;

import java.util.Arrays;
import java.util.Collection;

import org.junit.runners.Parameterized;

public class MessageProcessorNotificationTestCase extends AbstractNotificationTestCase
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
        MuleClient client = new MuleClient(muleContext);
        assertNotNull(client.send("vm://in-single", "test", null));
        assertNotNull(client.send("vm://in-processorChain", "test", null));
        assertNotNull(client.send("vm://in-choice", "test", null));
        assertNotNull(client.send("vm://in-all", "test", null));
        assertNotNull(client.send("vm://in-foreach", "test", null));
        assertNotNull(client.send("vm://in-enricher", "test", null));
        assertNotNull(client.send("vm://in-filter", "test", null));
        assertNotNull(client.send("vm://in-catch", "test", null));
        assertNotNull(client.send("vm://in-rollback", "test", null));
        assertNotNull(client.send("vm://in-choice-es", "test", null));
    }

    @Override
    public RestrictedNode getSpecification()
    {
        return new Node()
                //singleMP
                .serial(prePost())

                //processorChain
                .serial(prePost()) //logger-1
                .serial(prePost()) //logger-2

                //choice
                .serial(pre()) //choice
                .serial(prePost())    //otherwise-logger
                .serial(post())

                //all
                .serial(pre())
                .serial(prePost())
                .serial(prePost())
                .serial(post())

                //foreach
                .serial(pre()) //foreach
                .serial(prePost())    //logger-loop-1
                .serial(prePost())    //logger-loop-2
                .serial(post())

                //enricher
                .serial(prePost()) //append-string
                .serial(pre()) //chain
                .serial(prePost())
                .serial(prePost())
                .serial(post())

                //filter
                .serial(pre())
                .serial(prePost())
                .serial(post())

                //catch-es
                .serial(prePost())
                .serial(prePost())

                //rollback-es
                .serial(prePost())
                .serial(prePost())
                .serial(prePost())

                //choice-es
                .serial(prePost())
                .serial(prePost())
                ;
    }

    private RestrictedNode pre()
    {
        return new Node(MessageProcessorNotification.class, MessageProcessorNotification.MESSAGE_PROCESSOR_PRE_INVOKE);
    }

    private RestrictedNode post()
    {
        return new Node(MessageProcessorNotification.class, MessageProcessorNotification.MESSAGE_PROCESSOR_POST_INVOKE);
    }

    private RestrictedNode prePost()
    {
        return new Node().serial(pre()).serial(post());
    }

    @Override
    public void validateSpecification(RestrictedNode spec) throws Exception
    {
    }
}
