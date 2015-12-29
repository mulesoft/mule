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
import static org.mule.tck.util.FlowTraceUtils.assertStackElements;
import static org.mule.tck.util.FlowTraceUtils.isFlowStackElement;

import org.mule.DefaultMuleMessage;
import org.mule.api.context.notification.MessageProcessorNotificationListener;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.util.FlowTraceUtils.FlowStackAsserter;
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

        FlowStackAsserter.stackToAssert = null;
        FlowStackAsyncAsserter.latch = new CountDownLatch(1);
    }

    @Test
    public void flowStatic() throws Exception
    {
        muleContext.getClient().send("vm://in-flowStatic", new DefaultMuleMessage("payload", muleContext));

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));

        assertStackElements(FlowStackAsserter.stackToAssert,
                isFlowStackElement("flow", "/flow/processors/0"),
                isFlowStackElement("flowStatic", "/flowStatic/processors/0"));
    }

    @Test
    public void subFlowStatic() throws Exception
    {
        muleContext.getClient().send("vm://in-subFlowStatic", new DefaultMuleMessage("payload", muleContext));

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));

        assertStackElements(FlowStackAsserter.stackToAssert,
                isFlowStackElement("subFlow", "/subFlowStatic/processors/0/subFlow/subprocessors/0"),
                isFlowStackElement("subFlowStatic", "/subFlowStatic/processors/0"));
    }

    @Test
    public void flowDynamic() throws Exception
    {
        muleContext.getClient().send("vm://in-flowDynamic", new DefaultMuleMessage("payload", muleContext));

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));

        assertStackElements(FlowStackAsserter.stackToAssert,
                isFlowStackElement("flow", "/flow/processors/0"),
                isFlowStackElement("flowDynamic", "/flowDynamic/processors/0"));
    }

    @Test
    public void subFlowDynamic() throws Exception
    {
        muleContext.getClient().send("vm://in-subFlowDynamic", new DefaultMuleMessage("payload", muleContext));

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));

        assertStackElements(FlowStackAsserter.stackToAssert,
                isFlowStackElement("subFlow", "/subFlowDynamic/processors/0/subFlow/subprocessors/0"),
                isFlowStackElement("subFlowDynamic", "/subFlowDynamic/processors/0"));
    }

    @Test
    public void secondFlowStatic() throws Exception
    {
        muleContext.getClient().send("vm://in-secondFlowStatic", new DefaultMuleMessage("payload", muleContext));

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));

        assertStackElements(FlowStackAsserter.stackToAssert,
                isFlowStackElement("flow", "/flow/processors/0"),
                isFlowStackElement("secondFlowStatic", "/secondFlowStatic/processors/1"));
    }

    @Test
    public void secondSubFlowStatic() throws Exception
    {
        muleContext.getClient().send("vm://in-secondSubFlowStatic", new DefaultMuleMessage("payload", muleContext));

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));

        assertStackElements(FlowStackAsserter.stackToAssert,
                isFlowStackElement("subFlow", "/secondSubFlowStatic/processors/1/subFlow/subprocessors/0"),
                isFlowStackElement("secondSubFlowStatic", "/secondSubFlowStatic/processors/1"));
    }

    @Test
    public void secondFlowDynamic() throws Exception
    {
        muleContext.getClient().send("vm://in-secondFlowDynamic", new DefaultMuleMessage("payload", muleContext));

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));

        assertStackElements(FlowStackAsserter.stackToAssert,
                isFlowStackElement("flow", "/flow/processors/0"),
                isFlowStackElement("secondFlowDynamic", "/secondFlowDynamic/processors/1"));
    }

    @Test
    public void secondSubFlowDynamic() throws Exception
    {
        muleContext.getClient().send("vm://in-secondSubFlowDynamic", new DefaultMuleMessage("payload", muleContext));

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));

        assertStackElements(FlowStackAsserter.stackToAssert,
                isFlowStackElement("subFlow", "/secondSubFlowDynamic/processors/1/subFlow/subprocessors/0"),
                isFlowStackElement("secondSubFlowDynamic", "/secondSubFlowDynamic/processors/1"));
    }

    @Test
    public void flowStaticWithAsync() throws Exception
    {
        muleContext.getClient().send("vm://in-flowStaticWithAsync", new DefaultMuleMessage("payload", muleContext));

        FlowStackAsyncAsserter.latch.await(1, TimeUnit.SECONDS);

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));

        assertStackElements(FlowStackAsserter.stackToAssert,
                isFlowStackElement("flowInAsync", "/flowInAsync/processors/0"),
                isFlowStackElement("flowStaticWithAsync", "/flowStaticWithAsync/processors/0/0"));
    }

    @Test
    public void subFlowStaticWithAsync() throws Exception
    {
        muleContext.getClient().send("vm://in-subFlowStaticWithAsync", new DefaultMuleMessage("payload", muleContext));

        FlowStackAsyncAsserter.latch.await(1, TimeUnit.SECONDS);

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));

        assertStackElements(FlowStackAsserter.stackToAssert,
                isFlowStackElement("subFlowInAsync", "/subFlowStaticWithAsync/processors/0/0/subFlowInAsync/subprocessors/0"),
                isFlowStackElement("subFlowStaticWithAsync", "/subFlowStaticWithAsync/processors/0/0"));
    }

    @Test
    public void flowDynamicWithAsync() throws Exception
    {
        muleContext.getClient().send("vm://in-flowDynamicWithAsync", new DefaultMuleMessage("payload", muleContext));

        FlowStackAsyncAsserter.latch.await(1, TimeUnit.SECONDS);

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));

        assertStackElements(FlowStackAsserter.stackToAssert,
                isFlowStackElement("flowInAsync", "/flowInAsync/processors/0"),
                isFlowStackElement("flowDynamicWithAsync", "/flowDynamicWithAsync/processors/0/0"));
    }

    @Test
    public void subFlowDynamicWithAsync() throws Exception
    {
        muleContext.getClient().send("vm://in-subFlowDynamicWithAsync", new DefaultMuleMessage("payload", muleContext));

        FlowStackAsyncAsserter.latch.await(1, TimeUnit.SECONDS);

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));

        assertStackElements(FlowStackAsserter.stackToAssert,
                isFlowStackElement("subFlowInAsync", "/subFlowDynamicWithAsync/processors/0/0/subFlowInAsync/subprocessors/0"),
                isFlowStackElement("subFlowDynamicWithAsync", "/subFlowDynamicWithAsync/processors/0/0"));
    }

    @Test
    public void flowStaticWithEnricher() throws Exception
    {
        muleContext.getClient().send("vm://in-flowStaticWithEnricher", new DefaultMuleMessage("payload", muleContext));

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));

        assertStackElements(FlowStackAsserter.stackToAssert,
                isFlowStackElement("flow", "/flow/processors/0"),
                isFlowStackElement("flowStaticWithEnricher", "/flowStaticWithEnricher/processors/0/0"));
    }

    @Test
    public void subFlowStaticWithEnricher() throws Exception
    {
        muleContext.getClient().send("vm://in-subFlowStaticWithEnricher", new DefaultMuleMessage("payload", muleContext));

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));

        assertStackElements(FlowStackAsserter.stackToAssert,
                isFlowStackElement("subFlow", "/subFlowStaticWithEnricher/processors/0/0/subFlow/subprocessors/0"),
                isFlowStackElement("subFlowStaticWithEnricher", "/subFlowStaticWithEnricher/processors/0"));
    }

    @Test
    public void flowDynamicWithEnricher() throws Exception
    {
        muleContext.getClient().send("vm://in-flowDynamicWithEnricher", new DefaultMuleMessage("payload", muleContext));

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));

        assertStackElements(FlowStackAsserter.stackToAssert,
                isFlowStackElement("flow", "/flow/processors/0"),
                isFlowStackElement("flowDynamicWithEnricher", "/flowDynamicWithEnricher/processors/0/0"));
    }

    @Test
    public void subFlowDynamicWithEnricher() throws Exception
    {
        muleContext.getClient().send("vm://in-subFlowDynamicWithEnricher", new DefaultMuleMessage("payload", muleContext));

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));

        assertStackElements(FlowStackAsserter.stackToAssert,
                isFlowStackElement("subFlow", "/subFlowDynamicWithEnricher/processors/0/0/subFlow/subprocessors/0"),
                isFlowStackElement("subFlowDynamicWithEnricher", "/subFlowDynamicWithEnricher/processors/0/0"));
    }

    @Test
    public void flowStaticWithChoice() throws Exception
    {
        muleContext.getClient().send("vm://in-flowStaticWithChoice", new DefaultMuleMessage("payload", muleContext));

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));

        assertStackElements(FlowStackAsserter.stackToAssert,
                isFlowStackElement("flow", "/flow/processors/0"),
                isFlowStackElement("flowStaticWithChoice", "/flowStaticWithChoice/processors/0/0/0"));
    }

    @Test
    public void subFlowStaticWithChoice() throws Exception
    {
        muleContext.getClient().send("vm://in-subFlowStaticWithChoice", new DefaultMuleMessage("payload", muleContext));

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));

        assertStackElements(FlowStackAsserter.stackToAssert,
                isFlowStackElement("subFlow", "/subFlowStaticWithChoice/processors/0/0/0/subFlow/subprocessors/0"),
                isFlowStackElement("subFlowStaticWithChoice", "/subFlowStaticWithChoice/processors/0/0/0"));
    }

    @Test
    public void flowDynamicWithChoice() throws Exception
    {
        muleContext.getClient().send("vm://in-flowDynamicWithChoice", new DefaultMuleMessage("payload", muleContext));

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));

        assertStackElements(FlowStackAsserter.stackToAssert,
                isFlowStackElement("flow", "/flow/processors/0"),
                isFlowStackElement("flowDynamicWithChoice", "/flowDynamicWithChoice/processors/0/0/0"));
    }

    @Test
    public void subFlowDynamicWithChoice() throws Exception
    {
        muleContext.getClient().send("vm://in-subFlowDynamicWithChoice", new DefaultMuleMessage("payload", muleContext));

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));

        assertStackElements(FlowStackAsserter.stackToAssert,
                isFlowStackElement("subFlow", "/subFlowDynamicWithChoice/processors/0/0/0/subFlow/subprocessors/0"),
                isFlowStackElement("subFlowDynamicWithChoice", "/subFlowDynamicWithChoice/processors/0/0/0"));
    }

    @Test
    public void flowStaticWithScatterGather() throws Exception
    {
        muleContext.getClient().send("vm://in-flowStaticWithScatterGather", new DefaultMuleMessage("payload", muleContext));

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));

        assertStackElements(FlowStackAsserter.stackToAssert,
                isFlowStackElement("flow", "/flow/processors/0"),
                isFlowStackElement("flowStaticWithScatterGather", "/flowStaticWithScatterGather/processors/0/1/0"));
    }

    @Test
    public void subFlowStaticWithScatterGather() throws Exception
    {
        muleContext.getClient().send("vm://in-subFlowStaticWithScatterGather", new DefaultMuleMessage("payload", muleContext));

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));

        assertStackElements(FlowStackAsserter.stackToAssert,
                isFlowStackElement("subFlow", "/subFlowStaticWithScatterGather/processors/0/1/subFlow/subprocessors/0"),
                isFlowStackElement("subFlowStaticWithScatterGather", "/subFlowStaticWithScatterGather/processors/0/1"));
    }

    @Test
    public void flowDynamicWithScatterGather() throws Exception
    {
        muleContext.getClient().send("vm://in-flowDynamicWithScatterGather", new DefaultMuleMessage("payload", muleContext));

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));

        assertStackElements(FlowStackAsserter.stackToAssert,
                isFlowStackElement("flow", "/flow/processors/0"),
                isFlowStackElement("flowDynamicWithScatterGather", "/flowDynamicWithScatterGather/processors/0/1/0"));
    }

    @Test
    public void subFlowDynamicWithScatterGather() throws Exception
    {
        muleContext.getClient().send("vm://in-subFlowDynamicWithScatterGather", new DefaultMuleMessage("payload", muleContext));

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));

        assertStackElements(FlowStackAsserter.stackToAssert,
                isFlowStackElement("subFlow", "/subFlowDynamicWithScatterGather/processors/0/1/0/subFlow/subprocessors/0"),
                isFlowStackElement("subFlowDynamicWithScatterGather", "/subFlowDynamicWithScatterGather/processors/0/1/0"));
    }

    @Test
    public void flowStaticWithScatterGatherChain() throws Exception
    {
        muleContext.getClient().send("vm://in-flowStaticWithScatterGatherChain", new DefaultMuleMessage("payload", muleContext));

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));

        assertStackElements(FlowStackAsserter.stackToAssert,
                isFlowStackElement("flow", "/flow/processors/0"),
                isFlowStackElement("flowStaticWithScatterGatherChain", "/flowStaticWithScatterGatherChain/processors/0/1/0"));
    }

    @Test
    public void subFlowStaticWithScatterGatherChain() throws Exception
    {
        muleContext.getClient().send("vm://in-subFlowStaticWithScatterGatherChain", new DefaultMuleMessage("payload", muleContext));

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));

        assertStackElements(FlowStackAsserter.stackToAssert,
                isFlowStackElement("subFlow", "/subFlowStaticWithScatterGatherChain/processors/0/1/0/subFlow/subprocessors/0"),
                isFlowStackElement("subFlowStaticWithScatterGatherChain", "/subFlowStaticWithScatterGatherChain/processors/0/1/0"));
    }

    @Test
    public void flowDynamicWithScatterGatherChain() throws Exception
    {
        muleContext.getClient().send("vm://in-flowDynamicWithScatterGatherChain", new DefaultMuleMessage("payload", muleContext));

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));

        assertStackElements(FlowStackAsserter.stackToAssert,
                isFlowStackElement("flow", "/flow/processors/0"),
                isFlowStackElement("flowDynamicWithScatterGatherChain", "/flowDynamicWithScatterGatherChain/processors/0/1/0"));
    }

    @Test
    public void subFlowDynamicWithScatterGatherChain() throws Exception
    {
        muleContext.getClient().send("vm://in-subFlowDynamicWithScatterGatherChain", new DefaultMuleMessage("payload", muleContext));

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));
        
        assertStackElements(FlowStackAsserter.stackToAssert,
                isFlowStackElement("subFlow", "/subFlowDynamicWithScatterGatherChain/processors/0/1/0/subFlow/subprocessors/0"),
                isFlowStackElement("subFlowDynamicWithScatterGatherChain", "/subFlowDynamicWithScatterGatherChain/processors/0/1/0"));
    }
}
