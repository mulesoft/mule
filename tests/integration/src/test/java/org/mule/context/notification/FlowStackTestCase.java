/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context.notification;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.api.config.MuleProperties.MULE_FLOW_TRACE;
import static org.mule.tck.util.FlowTraceUtils.FlowStackAsserter.stackToAssert;
import static org.mule.tck.util.FlowTraceUtils.assertStackElements;
import static org.mule.tck.util.FlowTraceUtils.isFlowStackElement;

import org.mule.DefaultMuleMessage;
import org.mule.api.context.notification.MessageProcessorNotificationListener;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.util.FlowTraceUtils.FlowStackAsyncAsserter;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class FlowStackTestCase extends FunctionalTestCase
{
    @Rule
    public SystemProperty flowTraceEnabled = new SystemProperty(MULE_FLOW_TRACE, "true");

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/notifications/flow-stack-config.xml";
    }

    @Before
    public void before()
    {
        muleContext.getNotificationManager().addInterfaceToType(
                MessageProcessorNotificationListener.class,
                MessageProcessorNotification.class);

        stackToAssert = null;
        FlowStackAsyncAsserter.latch = new CountDownLatch(1);
    }

    @Test
    public void flowStatic() throws Exception
    {
        muleContext.getClient().send("vm://in-flowStatic", new DefaultMuleMessage(TEST_MESSAGE, muleContext));

        assertThat(stackToAssert, not(nullValue()));

        assertStackElements(stackToAssert,
                isFlowStackElement("flow", "/flow/processors/0"),
                isFlowStackElement("flowStatic", "/flowStatic/processors/0"));
    }

    @Test
    public void subFlowStatic() throws Exception
    {
        muleContext.getClient().send("vm://in-subFlowStatic", new DefaultMuleMessage(TEST_MESSAGE, muleContext));

        assertThat(stackToAssert, not(nullValue()));

        assertStackElements(stackToAssert,
                isFlowStackElement("subFlow", "/subFlowStatic/processors/0/subFlow/subprocessors/0"),
                isFlowStackElement("subFlowStatic", "/subFlowStatic/processors/0"));
    }

    @Test
    public void flowDynamic() throws Exception
    {
        muleContext.getClient().send("vm://in-flowDynamic", new DefaultMuleMessage(TEST_MESSAGE, muleContext));

        assertThat(stackToAssert, not(nullValue()));

        assertStackElements(stackToAssert,
                isFlowStackElement("flow", "/flow/processors/0"),
                isFlowStackElement("flowDynamic", "/flowDynamic/processors/0"));
    }

    @Test
    public void subFlowDynamic() throws Exception
    {
        muleContext.getClient().send("vm://in-subFlowDynamic", new DefaultMuleMessage(TEST_MESSAGE, muleContext));

        assertThat(stackToAssert, not(nullValue()));

        assertStackElements(stackToAssert,
                isFlowStackElement("subFlow", "/subFlowDynamic/processors/0/subFlow/subprocessors/0"),
                isFlowStackElement("subFlowDynamic", "/subFlowDynamic/processors/0"));
    }

    @Test
    public void secondFlowStatic() throws Exception
    {
        muleContext.getClient().send("vm://in-secondFlowStatic", new DefaultMuleMessage(TEST_MESSAGE, muleContext));

        assertThat(stackToAssert, not(nullValue()));

        assertStackElements(stackToAssert,
                isFlowStackElement("flow", "/flow/processors/0"),
                isFlowStackElement("secondFlowStatic", "/secondFlowStatic/processors/1"));
    }

    @Test
    public void secondSubFlowStatic() throws Exception
    {
        muleContext.getClient().send("vm://in-secondSubFlowStatic", new DefaultMuleMessage(TEST_MESSAGE, muleContext));

        assertThat(stackToAssert, not(nullValue()));

        assertStackElements(stackToAssert,
                isFlowStackElement("subFlow", "/secondSubFlowStatic/processors/1/subFlow/subprocessors/0"),
                isFlowStackElement("secondSubFlowStatic", "/secondSubFlowStatic/processors/1"));
    }

    @Test
    public void secondFlowDynamic() throws Exception
    {
        muleContext.getClient().send("vm://in-secondFlowDynamic", new DefaultMuleMessage(TEST_MESSAGE, muleContext));

        assertThat(stackToAssert, not(nullValue()));

        assertStackElements(stackToAssert,
                isFlowStackElement("flow", "/flow/processors/0"),
                isFlowStackElement("secondFlowDynamic", "/secondFlowDynamic/processors/1"));
    }

    @Test
    public void secondSubFlowDynamic() throws Exception
    {
        muleContext.getClient().send("vm://in-secondSubFlowDynamic", new DefaultMuleMessage(TEST_MESSAGE, muleContext));

        assertThat(stackToAssert, not(nullValue()));

        assertStackElements(stackToAssert,
                isFlowStackElement("subFlow", "/secondSubFlowDynamic/processors/1/subFlow/subprocessors/0"),
                isFlowStackElement("secondSubFlowDynamic", "/secondSubFlowDynamic/processors/1"));
    }

    @Test
    public void recursiveSubFlowDynamic() throws Exception
    {
        muleContext.getClient().send("vm://in-recursiveSubFlowDynamic", new DefaultMuleMessage(3, muleContext));

        assertThat(stackToAssert, not(nullValue()));

        assertStackElements(stackToAssert,
                isFlowStackElement("subFlow", "/recursiveSubFlowDynamic/processors/0/recursiveSubFlow/subprocessors/1/recursiveSubFlow/subprocessors/1/recursiveSubFlow/subprocessors/1/subFlow/subprocessors/0"),
                isFlowStackElement("recursiveSubFlow", "/recursiveSubFlowDynamic/processors/0/recursiveSubFlow/subprocessors/1/recursiveSubFlow/subprocessors/1/recursiveSubFlow/subprocessors/1"),
                isFlowStackElement("recursiveSubFlow", "/recursiveSubFlowDynamic/processors/0/recursiveSubFlow/subprocessors/1/recursiveSubFlow/subprocessors/1"),
                isFlowStackElement("recursiveSubFlow", "/recursiveSubFlowDynamic/processors/0/recursiveSubFlow/subprocessors/1"),
                isFlowStackElement("recursiveSubFlowDynamic", "/recursiveSubFlowDynamic/processors/0"));
    }

    @Test
    public void subSubFlowDynamic() throws Exception
    {
        muleContext.getClient().send("vm://in-subSubFlowDynamic", new DefaultMuleMessage("1", muleContext));

        assertThat(stackToAssert, not(nullValue()));

        assertStackElements(stackToAssert,
                isFlowStackElement("subFlow", "/subSubFlowDynamic/processors/0/subFlowSubRef1/subprocessors/0/subFlow/subprocessors/0"),
                isFlowStackElement("subFlowSubRef1", "/subSubFlowDynamic/processors/0/subFlowSubRef1/subprocessors/0"),
                isFlowStackElement("subSubFlowDynamic", "/subSubFlowDynamic/processors/0"));

        muleContext.getClient().send("vm://in-subSubFlowDynamic", new DefaultMuleMessage("2", muleContext));

        assertThat(stackToAssert, not(nullValue()));

        assertStackElements(stackToAssert,
                isFlowStackElement("subFlow", "/subSubFlowDynamic/processors/0/subFlowSubRef2/subprocessors/0/subFlow/subprocessors/0"),
                isFlowStackElement("subFlowSubRef2", "/subSubFlowDynamic/processors/0/subFlowSubRef2/subprocessors/0"),
                isFlowStackElement("subSubFlowDynamic", "/subSubFlowDynamic/processors/0"));
    }

    @Test
    public void flowStaticWithAsync() throws Exception
    {
        muleContext.getClient().send("vm://in-flowStaticWithAsync", new DefaultMuleMessage(TEST_MESSAGE, muleContext));

        FlowStackAsyncAsserter.latch.await(1, TimeUnit.SECONDS);

        assertThat(stackToAssert, not(nullValue()));

        assertStackElements(stackToAssert,
                isFlowStackElement("flowInAsync", "/flowInAsync/processors/0"),
                isFlowStackElement("flowStaticWithAsync", "/flowStaticWithAsync/processors/0/0"));
    }

    @Test
    public void subFlowStaticWithAsync() throws Exception
    {
        muleContext.getClient().send("vm://in-subFlowStaticWithAsync", new DefaultMuleMessage(TEST_MESSAGE, muleContext));

        FlowStackAsyncAsserter.latch.await(1, TimeUnit.SECONDS);

        assertThat(stackToAssert, not(nullValue()));

        assertStackElements(stackToAssert,
                isFlowStackElement("subFlowInAsync", "/subFlowStaticWithAsync/processors/0/0/subFlowInAsync/subprocessors/0"),
                isFlowStackElement("subFlowStaticWithAsync", "/subFlowStaticWithAsync/processors/0/0"));
    }

    @Test
    public void flowDynamicWithAsync() throws Exception
    {
        muleContext.getClient().send("vm://in-flowDynamicWithAsync", new DefaultMuleMessage(TEST_MESSAGE, muleContext));

        FlowStackAsyncAsserter.latch.await(1, TimeUnit.SECONDS);

        assertThat(stackToAssert, not(nullValue()));

        assertStackElements(stackToAssert,
                isFlowStackElement("flowInAsync", "/flowInAsync/processors/0"),
                isFlowStackElement("flowDynamicWithAsync", "/flowDynamicWithAsync/processors/0/0"));
    }

    @Test
    public void subFlowDynamicWithAsync() throws Exception
    {
        muleContext.getClient().send("vm://in-subFlowDynamicWithAsync", new DefaultMuleMessage(TEST_MESSAGE, muleContext));

        FlowStackAsyncAsserter.latch.await(1, TimeUnit.SECONDS);

        assertThat(stackToAssert, not(nullValue()));

        assertStackElements(stackToAssert,
                isFlowStackElement("subFlowInAsync", "/subFlowDynamicWithAsync/processors/0/0/subFlowInAsync/subprocessors/0"),
                isFlowStackElement("subFlowDynamicWithAsync", "/subFlowDynamicWithAsync/processors/0/0"));
    }

    @Test
    public void flowStaticWithEnricher() throws Exception
    {
        muleContext.getClient().send("vm://in-flowStaticWithEnricher", new DefaultMuleMessage(TEST_MESSAGE, muleContext));

        assertThat(stackToAssert, not(nullValue()));

        assertStackElements(stackToAssert,
                isFlowStackElement("flow", "/flow/processors/0"),
                isFlowStackElement("flowStaticWithEnricher", "/flowStaticWithEnricher/processors/0/0"));
    }

    @Test
    public void subFlowStaticWithEnricher() throws Exception
    {
        muleContext.getClient().send("vm://in-subFlowStaticWithEnricher", new DefaultMuleMessage(TEST_MESSAGE, muleContext));

        assertThat(stackToAssert, not(nullValue()));

        assertStackElements(stackToAssert,
                isFlowStackElement("subFlow", "/subFlowStaticWithEnricher/processors/0/0/subFlow/subprocessors/0"),
                isFlowStackElement("subFlowStaticWithEnricher", "/subFlowStaticWithEnricher/processors/0"));
    }

    @Test
    public void flowDynamicWithEnricher() throws Exception
    {
        muleContext.getClient().send("vm://in-flowDynamicWithEnricher", new DefaultMuleMessage(TEST_MESSAGE, muleContext));

        assertThat(stackToAssert, not(nullValue()));

        assertStackElements(stackToAssert,
                isFlowStackElement("flow", "/flow/processors/0"),
                isFlowStackElement("flowDynamicWithEnricher", "/flowDynamicWithEnricher/processors/0/0"));
    }

    @Test
    public void subFlowDynamicWithEnricher() throws Exception
    {
        muleContext.getClient().send("vm://in-subFlowDynamicWithEnricher", new DefaultMuleMessage(TEST_MESSAGE, muleContext));

        assertThat(stackToAssert, not(nullValue()));

        assertStackElements(stackToAssert,
                isFlowStackElement("subFlow", "/subFlowDynamicWithEnricher/processors/0/0/subFlow/subprocessors/0"),
                isFlowStackElement("subFlowDynamicWithEnricher", "/subFlowDynamicWithEnricher/processors/0/0"));
    }

    @Test
    public void flowStaticWithChoice() throws Exception
    {
        muleContext.getClient().send("vm://in-flowStaticWithChoice", new DefaultMuleMessage(TEST_MESSAGE, muleContext));

        assertThat(stackToAssert, not(nullValue()));

        assertStackElements(stackToAssert,
                isFlowStackElement("flow", "/flow/processors/0"),
                isFlowStackElement("flowStaticWithChoice", "/flowStaticWithChoice/processors/0/0/0"));
    }

    @Test
    public void subFlowStaticWithChoice() throws Exception
    {
        muleContext.getClient().send("vm://in-subFlowStaticWithChoice", new DefaultMuleMessage(TEST_MESSAGE, muleContext));

        assertThat(stackToAssert, not(nullValue()));

        assertStackElements(stackToAssert,
                isFlowStackElement("subFlow", "/subFlowStaticWithChoice/processors/0/0/0/subFlow/subprocessors/0"),
                isFlowStackElement("subFlowStaticWithChoice", "/subFlowStaticWithChoice/processors/0/0/0"));
    }

    @Test
    public void flowDynamicWithChoice() throws Exception
    {
        muleContext.getClient().send("vm://in-flowDynamicWithChoice", new DefaultMuleMessage(TEST_MESSAGE, muleContext));

        assertThat(stackToAssert, not(nullValue()));

        assertStackElements(stackToAssert,
                isFlowStackElement("flow", "/flow/processors/0"),
                isFlowStackElement("flowDynamicWithChoice", "/flowDynamicWithChoice/processors/0/0/0"));
    }

    @Test
    public void subFlowDynamicWithChoice() throws Exception
    {
        muleContext.getClient().send("vm://in-subFlowDynamicWithChoice", new DefaultMuleMessage(TEST_MESSAGE, muleContext));

        assertThat(stackToAssert, not(nullValue()));

        assertStackElements(stackToAssert,
                isFlowStackElement("subFlow", "/subFlowDynamicWithChoice/processors/0/0/0/subFlow/subprocessors/0"),
                isFlowStackElement("subFlowDynamicWithChoice", "/subFlowDynamicWithChoice/processors/0/0/0"));
    }

    @Test
    public void recursiveSubFlowWithChoice() throws Exception
    {
        muleContext.getClient().send("vm://in-recursiveSubFlowWithChoice", new DefaultMuleMessage(5, muleContext));

        assertThat(stackToAssert, not(nullValue()));

        assertStackElements(stackToAssert,
                isFlowStackElement("subFlow", "/recursiveSubFlowWithChoice/processors/0/recursiveSubFlowChoice/subprocessors/1/1/0/subFlow/subprocessors/0"),
                isFlowStackElement("recursiveSubFlowChoice", "/recursiveSubFlowWithChoice/processors/0/recursiveSubFlowChoice/subprocessors/1/1/0"),
                isFlowStackElement("recursiveSubFlowWithChoice", "/recursiveSubFlowWithChoice/processors/0"));
    }

    @Test
    public void flowStaticWithScatterGather() throws Exception
    {
        muleContext.getClient().send("vm://in-flowStaticWithScatterGather", new DefaultMuleMessage(TEST_MESSAGE, muleContext));

        assertThat(stackToAssert, not(nullValue()));

        assertStackElements(stackToAssert,
                isFlowStackElement("flow", "/flow/processors/0"),
                isFlowStackElement("flowStaticWithScatterGather", "/flowStaticWithScatterGather/processors/0/1/0"));
    }

    @Test
    public void subFlowStaticWithScatterGather() throws Exception
    {
        muleContext.getClient().send("vm://in-subFlowStaticWithScatterGather", new DefaultMuleMessage(TEST_MESSAGE, muleContext));

        assertThat(stackToAssert, not(nullValue()));

        assertStackElements(stackToAssert,
                isFlowStackElement("subFlow", "/subFlowStaticWithScatterGather/processors/0/1/subFlow/subprocessors/0"),
                isFlowStackElement("subFlowStaticWithScatterGather", "/subFlowStaticWithScatterGather/processors/0/1"));
    }

    @Test
    public void flowDynamicWithScatterGather() throws Exception
    {
        muleContext.getClient().send("vm://in-flowDynamicWithScatterGather", new DefaultMuleMessage(TEST_MESSAGE, muleContext));

        assertThat(stackToAssert, not(nullValue()));

        assertStackElements(stackToAssert,
                isFlowStackElement("flow", "/flow/processors/0"),
                isFlowStackElement("flowDynamicWithScatterGather", "/flowDynamicWithScatterGather/processors/0/1/0"));
    }

    @Test
    public void subFlowDynamicWithScatterGather() throws Exception
    {
        muleContext.getClient().send("vm://in-subFlowDynamicWithScatterGather", new DefaultMuleMessage(TEST_MESSAGE, muleContext));

        assertThat(stackToAssert, not(nullValue()));

        assertStackElements(stackToAssert,
                isFlowStackElement("subFlow", "/subFlowDynamicWithScatterGather/processors/0/1/0/subFlow/subprocessors/0"),
                isFlowStackElement("subFlowDynamicWithScatterGather", "/subFlowDynamicWithScatterGather/processors/0/1/0"));
    }

    @Test
    public void flowStaticWithScatterGatherChain() throws Exception
    {
        muleContext.getClient().send("vm://in-flowStaticWithScatterGatherChain", new DefaultMuleMessage(TEST_MESSAGE, muleContext));

        assertThat(stackToAssert, not(nullValue()));

        assertStackElements(stackToAssert,
                isFlowStackElement("flow", "/flow/processors/0"),
                isFlowStackElement("flowStaticWithScatterGatherChain", "/flowStaticWithScatterGatherChain/processors/0/1/0"));
    }

    @Test
    public void subFlowStaticWithScatterGatherChain() throws Exception
    {
        muleContext.getClient().send("vm://in-subFlowStaticWithScatterGatherChain", new DefaultMuleMessage(TEST_MESSAGE, muleContext));

        assertThat(stackToAssert, not(nullValue()));

        assertStackElements(stackToAssert,
                isFlowStackElement("subFlow", "/subFlowStaticWithScatterGatherChain/processors/0/1/0/subFlow/subprocessors/0"),
                isFlowStackElement("subFlowStaticWithScatterGatherChain", "/subFlowStaticWithScatterGatherChain/processors/0/1/0"));
    }

    @Test
    public void flowDynamicWithScatterGatherChain() throws Exception
    {
        muleContext.getClient().send("vm://in-flowDynamicWithScatterGatherChain", new DefaultMuleMessage(TEST_MESSAGE, muleContext));

        assertThat(stackToAssert, not(nullValue()));

        assertStackElements(stackToAssert,
                isFlowStackElement("flow", "/flow/processors/0"),
                isFlowStackElement("flowDynamicWithScatterGatherChain", "/flowDynamicWithScatterGatherChain/processors/0/1/0"));
    }

    @Test
    public void subFlowDynamicWithScatterGatherChain() throws Exception
    {
        muleContext.getClient().send("vm://in-subFlowDynamicWithScatterGatherChain", new DefaultMuleMessage(TEST_MESSAGE, muleContext));

        assertThat(stackToAssert, not(nullValue()));
        
        assertStackElements(stackToAssert,
                isFlowStackElement("subFlow", "/subFlowDynamicWithScatterGatherChain/processors/0/1/0/subFlow/subprocessors/0"),
                isFlowStackElement("subFlowDynamicWithScatterGatherChain", "/subFlowDynamicWithScatterGatherChain/processors/0/1/0"));
    }


    @Test
    public void flowChainedFilter() throws Exception
    {
        muleContext.getClient().send("vm://in-flowChainedFilter", new DefaultMuleMessage(TEST_MESSAGE, muleContext));

        assertThat(stackToAssert, not(nullValue()));

        assertStackElements(stackToAssert,
                isFlowStackElement("flow", "/flow/processors/0"),
                isFlowStackElement("flowChainedFilter", "/flowChainedFilter/processors/0/1"));
    }

    @Test
    public void flowChainedFilterManyProcessors() throws Exception
    {
        muleContext.getClient().send("vm://in-flowChainedFilterManyProcessors", new DefaultMuleMessage(TEST_MESSAGE, muleContext));

        assertThat(stackToAssert, not(nullValue()));

        assertStackElements(stackToAssert,
                isFlowStackElement("flow", "/flow/processors/0"),
                isFlowStackElement("flowChainedFilterManyProcessors", "/flowChainedFilterManyProcessors/processors/0/2"));
    }

    @Test
    public void flowUnacceptedMessageFilterUnaccepted() throws Exception
    {
        muleContext.getClient().send("vm://in-flowStaticWithMessageFilterUnaccepted", new DefaultMuleMessage(null, muleContext));
        assertThat(stackToAssert, not(nullValue()));

        assertStackElements(stackToAssert,
                            isFlowStackElement("flow", "/flow/processors/0"),
                            isFlowStackElement("flowStaticWithMessageFilterUnaccepted", "/flowStaticWithMessageFilterUnaccepted/processors/0"));
    }

    @Test
    public void subFlowUnacceptedMessageFilterUnaccepted() throws Exception
    {
        muleContext.getClient().send("vm://in-subFlowStaticWithMessageFilterUnaccepted", new DefaultMuleMessage(null, muleContext));
        assertThat(stackToAssert, not(nullValue()));

        assertStackElements(stackToAssert,
                            isFlowStackElement("subFlow", "/subFlowStaticWithMessageFilterUnaccepted/processors/0/0/subFlow/subprocessors/0"),
                            isFlowStackElement("subFlowStaticWithMessageFilterUnaccepted", "/subFlowStaticWithMessageFilterUnaccepted/processors/0"));
    }

    @Test
    public void flowForEach() throws Exception
    {
        muleContext.getClient().send("vm://in-flowForEach", new DefaultMuleMessage(TEST_MESSAGE, muleContext));

        assertThat(stackToAssert, not(nullValue()));

        assertStackElements(stackToAssert,
                isFlowStackElement("flow", "/flow/processors/0"),
                isFlowStackElement("flowForEach", "/flowForEach/processors/0/1"));
    }

    @Test
    public void flowForEachRepeatedInterceptingMessageProcessor() throws Exception
    {
        muleContext.getClient().send("vm://in-flowForEachRepeatedInterceptingMessageProcessor", new DefaultMuleMessage(TEST_MESSAGE, muleContext));

        assertThat(stackToAssert, not(nullValue()));

        assertStackElements(stackToAssert,
                isFlowStackElement("flow", "/flow/processors/0"),
                isFlowStackElement("flowForEachRepeatedInterceptingMessageProcessor", "/flowForEachRepeatedInterceptingMessageProcessor/processors/0/3"));
    }

    @Test
    public void flowForEachFilter() throws Exception
    {
        muleContext.getClient().send("vm://in-flowForEachFilter", new DefaultMuleMessage(TEST_MESSAGE, muleContext));

        assertThat(stackToAssert, not(nullValue()));

        assertStackElements(stackToAssert,
                isFlowStackElement("flow", "/flow/processors/0"),
                isFlowStackElement("flowForEachFilter", "/flowForEachFilter/processors/0/1"));
    }

}
