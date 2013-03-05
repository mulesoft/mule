/*
 * $Id: MessageProcessorNotificationPathTestCase.java 25187 2013-01-11 16:54:29Z luciano.gandini $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context.notification;

import org.junit.Assert;
import org.junit.Test;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.Pipeline;
import org.mule.api.processor.DefaultMessageProcessorPathElement;
import org.mule.api.processor.MessageProcessor;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.NotificationUtils;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

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
        testFlowPaths("singleMP2", "/0","/1");
        testFlowPaths("singleMP3", "/0","/1","/2");
        testFlowPaths("processorChain2", "/0", "/0/0", "/0/1", "/0/2");
        testFlowPaths("processorChain3", "/0", "/0/0", "/0/1");
        testFlowPaths("processorChain", "/0", "/0/0", "/0/1");
        testFlowPaths("customProcessor", "/0", "/1");
    }

    @Test
    public void routers() throws Exception
    {
        testFlowPaths("choice", "/0", "/0/0", "/0/0/0", "/0/1", "/0/1/0", "/0/2", "/0/2/0");
        testFlowPaths("all2", "/0", "/0/0", "/0/0/0","/0/0/1", "/0/1", "/0/1/0","/0/1/1", "/1");
        testFlowPaths("choice2", "/0", "/0/0", "/0/0/0","/0/0/1", "/0/1", "/0/1/0", "/0/2", "/0/2/0","/0/2/1");
        testFlowPaths("all", "/0", "/0/0", "/0/0/0", "/0/1", "/0/1/0", "/1");
    }

    @Test
    public void scopes() throws Exception
    {
        testFlowPaths("foreach", "/0", "/0/0", "/1");
        testFlowPaths("enricher", "/0", "/0/0", "/1", "/1/0", "/1/0/0", "/1/0/1");
        testFlowPaths("until-successful", "/0", "/0/0", "/0/0/0", "/0/0/1");
        testFlowPaths("async", "/0", "/0/0", "/0/1");
    }

    @Test
    public void filters() throws Exception
    {
        testFlowPaths("filters", "/0", "/1");
        testFlowPaths("idempotent-msg-filter", "/0", "/1" );
        testFlowPaths("idempotent-secure-hash-msg-filter", "/0", "/1" );
    }

    @Test
    public void flowRefs() throws Exception
    {
        testFlowPaths("subflow", "/0", "/1", "/1/subflow-call/subprocessors/0", "/1/subflow-call/subprocessors/1");
    }

    @Test
    public void exceptionStrategies() throws Exception
    {
        testFlowPaths("catch-es", "/0", "es/0");
        testFlowPaths("rollback-es", "/0", "es/0", "es/1");
        testFlowPaths("choice-es", "/0", "es/0/0", "es/0/1", "es/1/0");
        testFlowPaths("global-es", "/0", "Global_Exception_Strategy/es/0", "Global_Exception_Strategy/es/1");

    }

    @Test
    public void requestReply() throws Exception
    {
        testFlowPaths("request-reply", "/0", "/1" );
    }

    @Test
    public void multipleEndpoints() throws Exception
    {
        testFlowPaths("composite-source", "/0" );
        testFlowPaths("first-successful", "/0", "/1", "/1/0", "/1/1", "/1/2", "/1/3" );
        testFlowPaths("round-robin", "/0", "/0/0", "/0/1", "/0/2", "/1");
    }

    @Test
    public void collections() throws Exception
    {
        testFlowPaths("collectionAggregator", "/0", "/1", "/2");
        testFlowPaths("customAggregator", "/0", "/1", "/2");
        testFlowPaths("chunkAggregator", "/0", "/1", "/2", "/3");
        testFlowPaths("combineCollections", "/0", "/1");
    }

    @Test
    public void wireTap() throws Exception
    {
        testFlowPaths("wire-tap", "/0", "/0/0", "/1");
    }


    private void testFlowPaths(String flowName, String... nodes) throws Exception
    {
        String[] expectedPaths = generatePaths(flowName, nodes);
        FlowConstruct flow = getFlowConstruct(flowName);
        DefaultMessageProcessorPathElement flowElement = new DefaultMessageProcessorPathElement(null, flowName);
        ((Pipeline) flow).addMessageProcessorPathElements(flowElement);
        Map<MessageProcessor, String> messageProcessorPaths = NotificationUtils.buildPaths(flowElement);
        String[] flowPaths = messageProcessorPaths.values().toArray(new String[]{});
        Assert.assertArrayEquals(expectedPaths, flowPaths);
    }

    private String[] generatePaths(String flowName, String[] nodes)
    {
        Set<String> pathSet = new LinkedHashSet<String>();
        String base = "/" + flowName + "/processors";
        for (String node : nodes)
        {
            if (!node.startsWith("/"))
            {
                base = "/" + flowName + "/";
            }
            pathSet.add(base + node);
        }
        return pathSet.toArray(new String[0]);
    }
}
