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
import org.junit.Test;
import org.junit.runners.Parameterized;

public class MulticastingRouterNotificationTestCase extends AbstractMessageProcessorNotificationTestCase
{
    public MulticastingRouterNotificationTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {{ConfigVariant.FLOW,
                                              "org/mule/test/integration/notifications/message-processor-notification-test-flow.xml"}});
    }

    private MuleClient client;

    @Before
    public void before()
    {
        client = muleContext.getClient();
    }

    private Factory specificationFactory;

    @Test
    public void all() throws Exception
    {
        specificationFactory = new Factory()
        {

            @Override
            public Object create()
            {
                return new Node()
                                 .serial(pre()) // Two routes with chain with one element
                                 .serial(prePost())
                                 .serial(prePost())
                                 .serial(post())
                                 .serial(prePost()) // MP after the Scope;
                ;
            }
        };

        assertNotNull(client.send("vm://in-all", "test", null));

        assertNotifications();
    }

    @Test
    public void all2() throws Exception
    {
        specificationFactory = new Factory()
        {

            @Override
            public Object create()
            {
                return new Node()
                                 /* All */.serial(pre()) // Two routes with chain with two first one is interceptiong
                                                         // elements
                                 /* CollectionSplitter */.serial(pre())
                                 /* Logger */.serial(prePost())
                                 /* CollectionSplitter */.serial(post())
                                 /* CollectionSplitter */.serial(pre())
                                 /* Logger */.serial(prePost())
                                 /* CollectionSplitter */.serial(post())
                                 /* All */.serial(post())
                                 /* Logger */.serial(prePost()) // MP after the Scope;
                ;
            }
        };

        List<String> testList = Arrays.asList("test");
        assertNotNull(client.send("vm://in-all2", testList, null));

        assertNotifications();
    }

    @Test
    public void all3() throws Exception
    {
        specificationFactory = new Factory()
        {

            @Override
            public Object create()
            {
                return new Node()
                                 .serial(pre()) // Two routes with no chain with one element
                                 .serial(prePost())
                                 .serial(prePost())
                                 .serial(post())
                                 .serial(prePost()) // MP after the Scope;
                ;
            }
        };

        assertNotNull(client.send("vm://in-all3", "test", null));

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
