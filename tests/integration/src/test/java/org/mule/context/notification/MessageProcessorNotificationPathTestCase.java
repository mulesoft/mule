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
    public void components() throws Exception
    {
        testFlowPaths("singleMP", "/0");
        testFlowPaths("processorChain", "/0/0", "/0/1");
    }

    @Test
    public void routers() throws Exception
    {
        testFlowPaths("choice", "/0/0/0", "/0/1/0", "/0/2/0");
        testFlowPaths("all", "/0/0/0", "/0/1/0");
    }

    @Test
    public void scopes() throws Exception
    {
        testFlowPaths("foreach", "/0/0");
        testFlowPaths("enricher", "/0/0", "/1/0/0", "/1/0/1");
        testFlowPaths("until-successful", "/0/0/0", "/0/0/1");
        //testFlowPaths("async", "/0/0", "/0/1");
    }

    @Test
    public void filters() throws Exception
    {
        testFlowPaths("filters", "/0", "/1");
    }

    @Test
    public void exceptionStrategies() throws Exception
    {
        testFlowPaths("catch-es", "/0", "es/0");
        testFlowPaths("rollback-es", "/0", "es/0");
        testFlowPaths("choice-es", "/0", "es/0", "es/1");
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
            if (leaf.startsWith("es/"))
            {
                base = "/" + flowName + "/es";
            }
            String prefix = "/";
            for (String part : leaf.substring(leaf.indexOf("/") + 1).split("/"))
            {
                pathSet.add(base + prefix + part);
                prefix += part + "/";
            }
        }
        return pathSet.toArray(new String[0]);
    }
}
