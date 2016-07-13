/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.context.notification.processors;

import static org.junit.Assert.assertNotNull;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.test.core.context.notification.Node;
import org.mule.test.core.context.notification.RestrictedNode;

import java.util.Arrays;
import java.util.List;

import org.junit.Ignore;

@Ignore("ArtifactClassloaderTestRunner CXF issue when running all tests, works when executed isolated")
public class MulticastingRouterNotificationTestCase extends AbstractMessageProcessorNotificationTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/notifications/message-processor-notification-test-flow.xml";
    }

    @Override
    public void doTest() throws Exception
    {
        List<String> testList = Arrays.asList(AbstractMuleContextTestCase.TEST_PAYLOAD);
        assertNotNull(flowRunner("all").withPayload(AbstractMuleContextTestCase.TEST_PAYLOAD).run());
        assertNotNull(flowRunner("all2").withPayload(testList).run());
        assertNotNull(flowRunner("all3").withPayload(AbstractMuleContextTestCase.TEST_PAYLOAD).run());
    }


    @Override
    public RestrictedNode getSpecification()
    {
        return new Node()
                .serial(pre())      //Two routes with chain with one element
                .serial(prePost())
                .serial(prePost())
                .serial(post())
                .serial(prePost())    //MP after the Scope;

                /*All*/.serial(pre())      //Two routes with chain with two first one is interceptiong elements
                /*CollectionSplitter*/.serial(pre())
                /* Logger           */.serial(prePost())
                /*CollectionSplitter*/.serial(post())
                /*CollectionSplitter*/.serial(pre())
                /* Logger           */.serial(prePost())
                /*CollectionSplitter*/.serial(post())
                /*All*/.serial(post())
                /*Logger*/.serial(prePost())    //MP after the Scope;

                .serial(pre())       //Two routes with no chain with one element
                .serial(prePost())
                .serial(prePost())
                .serial(post())
                .serial(prePost())    //MP after the Scope;
                ;
    }

    @Override
    public void validateSpecification(RestrictedNode spec) throws Exception
    {
    }
}
