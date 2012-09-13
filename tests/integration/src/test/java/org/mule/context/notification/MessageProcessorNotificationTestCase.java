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
        assertNotNull(client.send("vm://in-1", "test", null));
        assertNotNull(client.send("vm://in-2", "test", null));
        assertNotNull(client.send("vm://in-3", "test", null));
        assertNotNull(client.send("vm://in-4", "test", null));
    }

    @Override
    public RestrictedNode getSpecification()
    {
        return new Node()
                //singleMP
                .serial(new Node(MessageProcessorNotification.class, MessageProcessorNotification.MESSAGE_PROCESSOR_PRE_INVOKE))
                .serial(new Node(MessageProcessorNotification.class, MessageProcessorNotification.MESSAGE_PROCESSOR_POST_INVOKE))
                //foreach
                .serial(new Node(MessageProcessorNotification.class, MessageProcessorNotification.MESSAGE_PROCESSOR_PRE_INVOKE)) //foreach
                .serial(new Node(MessageProcessorNotification.class, MessageProcessorNotification.MESSAGE_PROCESSOR_PRE_INVOKE))    //logger-loop-1
                .serial(new Node(MessageProcessorNotification.class, MessageProcessorNotification.MESSAGE_PROCESSOR_POST_INVOKE))
                .serial(new Node(MessageProcessorNotification.class, MessageProcessorNotification.MESSAGE_PROCESSOR_PRE_INVOKE))    //logger-loop-2
                .serial(new Node(MessageProcessorNotification.class, MessageProcessorNotification.MESSAGE_PROCESSOR_POST_INVOKE))
                .serial(new Node(MessageProcessorNotification.class, MessageProcessorNotification.MESSAGE_PROCESSOR_POST_INVOKE))
                //processorChain
                .serial(new Node(MessageProcessorNotification.class, MessageProcessorNotification.MESSAGE_PROCESSOR_PRE_INVOKE)) //logger-1
                .serial(new Node(MessageProcessorNotification.class, MessageProcessorNotification.MESSAGE_PROCESSOR_POST_INVOKE))
                .serial(new Node(MessageProcessorNotification.class, MessageProcessorNotification.MESSAGE_PROCESSOR_PRE_INVOKE)) //logger-2
                .serial(new Node(MessageProcessorNotification.class, MessageProcessorNotification.MESSAGE_PROCESSOR_POST_INVOKE))
                //choice
                .serial(new Node(MessageProcessorNotification.class, MessageProcessorNotification.MESSAGE_PROCESSOR_PRE_INVOKE)) //choice
                .serial(new Node(MessageProcessorNotification.class, MessageProcessorNotification.MESSAGE_PROCESSOR_PRE_INVOKE))    //otherwise-logger
                .serial(new Node(MessageProcessorNotification.class, MessageProcessorNotification.MESSAGE_PROCESSOR_POST_INVOKE))
                .serial(new Node(MessageProcessorNotification.class, MessageProcessorNotification.MESSAGE_PROCESSOR_POST_INVOKE));
    }

    @Override
    public void validateSpecification(RestrictedNode spec) throws Exception
    {
    }
}
