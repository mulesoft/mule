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

import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.Pipeline;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class MessageProcessorNotificationPathTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/notifications/message-processor-notification-test-flow.xml";
    }

    @Test
    public void singleMP() throws Exception
    {
        testFlowPaths("singleMP", "/0");
    }

    @Test
    public void foreach() throws Exception
    {
        testFlowPaths("foreach", "/0/0");
    }

    @Test
    public void processorChain() throws Exception
    {
        testFlowPaths("processorChain", "/0/0", "/0/1");
    }

    @Test
    public void choice() throws Exception
    {
        testFlowPaths("choice", "/0/0/0", "/0/1/0", "/0/2/0");
    }

    private void testFlowPaths(String flowName, String... leaves) throws Exception
    {
        String[] expectedPaths = generatePathsFromLeaves(flowName, leaves);
        FlowConstruct flow = getFlowConstruct(flowName);
        String[] flowPaths = ((Pipeline) flow).getProcessorPaths();
        Assert.assertArrayEquals(expectedPaths, flowPaths);
    }

    private String[] generatePathsFromLeaves(String flowName, String[] leaves)
    {
        Set<String> pathSet = new LinkedHashSet<String>();
        String base = "/" + flowName + "/processors";
        for (String leaf : leaves)
        {
            String prefix = "/";
            for (String part : leaf.split("/"))
            {
                if (part.length()==0) continue;
                pathSet.add(base + prefix + part);
                prefix += part + "/";
            }
        }
        return pathSet.toArray(new String[0]);
    }
}
