/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context.notification.processors;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.junit.Assert.assertThat;
import static org.mule.util.NotificationUtils.buildPathResolver;

import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.Pipeline;
import org.mule.api.processor.DefaultMessageProcessorPathElement;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.util.NotificationUtils.FlowMap;

import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Test;

/**
 *
 */
public class MessageProcessorNotificationPathTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
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
        testFlowPaths("processorChain4", "/0", "/0/0", "/0/1", "/1");
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
        testFlowPaths("scatterGather", "/0", "/0/0", "/0/0/0", "/0/1", "/0/1/0", "/0/1/1");
    }

    @Test
    public void interceptors() throws Exception
    {
         testFlowPaths("cxfMP","/0","/1","/2");
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
        testFlowPaths("subflow2", "/0", "/1", "/1/subflow-call/subprocessors/0", "/1/subflow-call/subprocessors/1","/2");
        testFlowPaths("subflow\\/With\\/Slash", "/0", "/1", "/1/subflow\\/call/subprocessors/0", "/1/subflow\\/call/subprocessors/1","/2");
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
        FlowConstruct flow = getFlowConstruct(unescape(flowName));
        DefaultMessageProcessorPathElement flowElement = new DefaultMessageProcessorPathElement(null, flowName);
        ((Pipeline) flow).addMessageProcessorPathElements(flowElement);
        FlowMap messageProcessorPaths = buildPathResolver(flowElement);

        assertThat(messageProcessorPaths.getAllPaths(), hasSize(nodes.length));
        assertThat(messageProcessorPaths.getAllPaths(), hasItems(expectedPaths));
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

    private String unescape(String name)
    {
        StringBuilder builder = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++)
        {
            char c = name.charAt(i);
            if (i < (name.length() - 1) && name.charAt(i + 1) == '/')
            {
                builder.append("/");
                i++;
            }
            else
            {
                builder.append(c);
            }
        }
        return builder.toString();
    }
}
