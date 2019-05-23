/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context.notification;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static org.mule.api.config.MuleProperties.MULE_FLOW_TRACE;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.notification.MessageProcessorNotificationListener;
import org.mule.api.context.notification.ProcessorsTrace;
import org.mule.api.processor.MessageProcessor;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class ProcessorsTraceTestCase extends FunctionalTestCase
{

    public static class ProcessorsTraceAsserter implements MessageProcessor
    {

        public static ProcessorsTrace processorsTraceToAssert;

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            processorsTraceToAssert = event.getProcessorsTrace();
            return event;
        }
    }

    public static class ProcessorsTraceAsyncAsserter extends ProcessorsTraceAsserter
    {

        public static CountDownLatch latch;

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            super.process(event);
            latch.countDown();
            return event;
        }
    }

    @Rule
    public SystemProperty flowTraceEnabled = new SystemProperty(MULE_FLOW_TRACE, "true");

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/notifications/processors-trace-config.xml";
    }

    @Before
    public void before()
    {
        muleContext.getNotificationManager().addInterfaceToType(
                MessageProcessorNotificationListener.class,
                MessageProcessorNotification.class);

        ProcessorsTraceAsserter.processorsTraceToAssert = null;
        ProcessorsTraceAsyncAsserter.latch = new CountDownLatch(1);
    }

    @Test
    public void flowStatic() throws Exception
    {
        muleContext.getClient().send("vm://in-flowStatic", new DefaultMuleMessage("payload", muleContext));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, not(nullValue()));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, hasExecutedProcessors(
                "/flowStatic/processors/0",
                "/flow/processors/0"));
    }

    @Test
    public void subFlowStatic() throws Exception
    {
        muleContext.getClient().send("vm://in-subFlowStatic", new DefaultMuleMessage("payload", muleContext));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, not(nullValue()));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, hasExecutedProcessors(
                "/subFlowStatic/processors/0",
                "/subFlowStatic/processors/0/subFlow/subprocessors/0"));
    }

    @Test
    public void flowDynamic() throws Exception
    {
        muleContext.getClient().send("vm://in-flowDynamic", new DefaultMuleMessage("payload", muleContext));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, not(nullValue()));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, hasExecutedProcessors(
                "/flowDynamic/processors/0",
                "/flow/processors/0"));
    }

    @Test
    public void subFlowDynamic() throws Exception
    {
        muleContext.getClient().send("vm://in-subFlowDynamic", new DefaultMuleMessage("payload", muleContext));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, not(nullValue()));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, hasExecutedProcessors(
                "/subFlowDynamic/processors/0",
                "/subFlowDynamic/processors/0/subFlow/subprocessors/0"));
    }

    @Test
    public void secondFlowStatic() throws Exception
    {
        muleContext.getClient().send("vm://in-secondFlowStatic", new DefaultMuleMessage("payload", muleContext));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, not(nullValue()));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, hasExecutedProcessors(
                "/secondFlowStatic/processors/0",
                "/flow/processors/0",
                "/secondFlowStatic/processors/1"));
    }

    @Test
    public void secondSubFlowStatic() throws Exception
    {
        muleContext.getClient().send("vm://in-secondSubFlowStatic", new DefaultMuleMessage("payload", muleContext));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, not(nullValue()));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, hasExecutedProcessors(
                "/secondSubFlowStatic/processors/0",
                "/secondSubFlowStatic/processors/0/subFlow/subprocessors/0",
                "/secondSubFlowStatic/processors/1",
                "/secondSubFlowStatic/processors/1/subFlow/subprocessors/0"));
    }

    @Test
    public void secondFlowDynamic() throws Exception
    {
        muleContext.getClient().send("vm://in-secondFlowDynamic", new DefaultMuleMessage("payload", muleContext));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, not(nullValue()));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, hasExecutedProcessors(
                "/secondFlowDynamic/processors/0",
                "/flow/processors/0",
                "/secondFlowDynamic/processors/1"));
    }

    @Test
    public void secondSubFlowDynamic() throws Exception
    {
        muleContext.getClient().send("vm://in-secondSubFlowDynamic", new DefaultMuleMessage("payload", muleContext));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, not(nullValue()));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, hasExecutedProcessors(
                "/secondSubFlowDynamic/processors/0",
                "/secondSubFlowDynamic/processors/0/subFlow/subprocessors/0",
                "/secondSubFlowDynamic/processors/1",
                "/secondSubFlowDynamic/processors/1/subFlow/subprocessors/0"));
    }

    @Test
    public void flowStaticWithAsync() throws Exception
    {
        muleContext.getClient().send("vm://in-flowStaticWithAsync", new DefaultMuleMessage("payload", muleContext));

        ProcessorsTraceAsyncAsserter.latch.await(1, TimeUnit.SECONDS);

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, not(nullValue()));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, hasExecutedProcessors(
                "/flowStaticWithAsync/processors/0",
                "/flowStaticWithAsync/processors/0/0",
                "/flowInAsync/processors/0"));
    }

    @Test
    public void subFlowStaticWithAsync() throws Exception
    {
        muleContext.getClient().send("vm://in-subFlowStaticWithAsync", new DefaultMuleMessage("payload", muleContext));

        ProcessorsTraceAsyncAsserter.latch.await(1, TimeUnit.SECONDS);

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, not(nullValue()));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, hasExecutedProcessors(
                "/subFlowStaticWithAsync/processors/0",
                "/subFlowStaticWithAsync/processors/0/0",
                "/subFlowStaticWithAsync/processors/0/0/subFlowInAsync/subprocessors/0"));
    }

    @Test
    public void flowDynamicWithAsync() throws Exception
    {
        muleContext.getClient().send("vm://in-flowDynamicWithAsync", new DefaultMuleMessage("payload", muleContext));

        ProcessorsTraceAsyncAsserter.latch.await(1, TimeUnit.SECONDS);

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, not(nullValue()));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, hasExecutedProcessors(
                "/flowDynamicWithAsync/processors/0",
                "/flowDynamicWithAsync/processors/0/0",
                "/flowInAsync/processors/0"));
    }

    @Test
    public void subFlowDynamicWithAsync() throws Exception
    {
        muleContext.getClient().send("vm://in-subFlowDynamicWithAsync", new DefaultMuleMessage("payload", muleContext));

        ProcessorsTraceAsyncAsserter.latch.await(1, TimeUnit.SECONDS);

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, not(nullValue()));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, hasExecutedProcessors(
                "/subFlowDynamicWithAsync/processors/0",
                "/subFlowDynamicWithAsync/processors/0/0",
                "/subFlowDynamicWithAsync/processors/0/0/subFlowInAsync/subprocessors/0"));
    }

    @Test
    public void flowStaticWithEnricher() throws Exception
    {
        muleContext.getClient().send("vm://in-flowStaticWithEnricher", new DefaultMuleMessage("payload", muleContext));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, not(nullValue()));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, hasExecutedProcessors(
                "/flowStaticWithEnricher/processors/0",
                "/flowStaticWithEnricher/processors/0/0",
                "/flow/processors/0"));
    }

    @Test
    public void subFlowStaticWithEnricher() throws Exception
    {
        muleContext.getClient().send("vm://in-subFlowStaticWithEnricher", new DefaultMuleMessage("payload", muleContext));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, not(nullValue()));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, hasExecutedProcessors(
                "/subFlowStaticWithEnricher/processors/0",
                "/subFlowStaticWithEnricher/processors/0/0/subFlow/subprocessors/0"));
    }

    @Test
    public void flowDynamicWithEnricher() throws Exception
    {
        muleContext.getClient().send("vm://in-flowDynamicWithEnricher", new DefaultMuleMessage("payload", muleContext));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, not(nullValue()));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, hasExecutedProcessors(
                "/flowDynamicWithEnricher/processors/0",
                "/flowDynamicWithEnricher/processors/0/0",
                "/flow/processors/0"));
    }

    @Test
    public void subFlowDynamicWithEnricher() throws Exception
    {
        muleContext.getClient().send("vm://in-subFlowDynamicWithEnricher", new DefaultMuleMessage("payload", muleContext));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, not(nullValue()));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, hasExecutedProcessors(
                "/subFlowDynamicWithEnricher/processors/0",
                "/subFlowDynamicWithEnricher/processors/0/0",
                "/subFlowDynamicWithEnricher/processors/0/0/subFlow/subprocessors/0"));
    }

    @Test
    public void flowStaticWithChoice() throws Exception
    {
        muleContext.getClient().send("vm://in-flowStaticWithChoice", new DefaultMuleMessage("payload", muleContext));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, not(nullValue()));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, hasExecutedProcessors(
                "/flowStaticWithChoice/processors/0",
                "/flowStaticWithChoice/processors/0/0/0",
                "/flow/processors/0"));
    }

    @Test
    public void subFlowStaticWithChoice() throws Exception
    {
        muleContext.getClient().send("vm://in-subFlowStaticWithChoice", new DefaultMuleMessage("payload", muleContext));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, not(nullValue()));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, hasExecutedProcessors(
                "/subFlowStaticWithChoice/processors/0",
                "/subFlowStaticWithChoice/processors/0/0/0",
                "/subFlowStaticWithChoice/processors/0/0/0/subFlow/subprocessors/0"));
    }

    @Test
    public void flowDynamicWithChoice() throws Exception
    {
        muleContext.getClient().send("vm://in-flowDynamicWithChoice", new DefaultMuleMessage("payload", muleContext));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, not(nullValue()));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, hasExecutedProcessors(
                "/flowDynamicWithChoice/processors/0",
                "/flowDynamicWithChoice/processors/0/0/0",
                "/flow/processors/0"));
    }

    @Test
    public void subFlowDynamicWithChoice() throws Exception
    {
        muleContext.getClient().send("vm://in-subFlowDynamicWithChoice", new DefaultMuleMessage("payload", muleContext));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, not(nullValue()));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, hasExecutedProcessors(
                "/subFlowDynamicWithChoice/processors/0",
                "/subFlowDynamicWithChoice/processors/0/0/0",
                "/subFlowDynamicWithChoice/processors/0/0/0/subFlow/subprocessors/0"));
    }

    @Test
    public void flowStaticWithScatterGather() throws Exception
    {
        muleContext.getClient().send("vm://in-flowStaticWithScatterGather", new DefaultMuleMessage("payload", muleContext));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, not(nullValue()));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, hasExecutedProcessorsNoOrder(
                "/flowStaticWithScatterGather/processors/0",
                "/flowStaticWithScatterGather/processors/0/0/0",
                "/flowStaticWithScatterGather/processors/0/1/0",
                "/flow/processors/0"));
    }

    @Test
    public void subFlowStaticWithScatterGather() throws Exception
    {
        muleContext.getClient().send("vm://in-subFlowStaticWithScatterGather", new DefaultMuleMessage("payload", muleContext));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, not(nullValue()));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, hasExecutedProcessorsNoOrder(
                "/subFlowStaticWithScatterGather/processors/0",
                "/subFlowStaticWithScatterGather/processors/0/0/0",
                "/subFlowStaticWithScatterGather/processors/0/1",
                "/subFlowStaticWithScatterGather/processors/0/1/subFlow/subprocessors/0"));
    }

    @Ignore("MULE-6926: flaky test")
    @Test
    public void flowDynamicWithScatterGather() throws Exception
    {
        muleContext.getClient().send("vm://in-flowDynamicWithScatterGather", new DefaultMuleMessage("payload", muleContext));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, not(nullValue()));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, hasExecutedProcessorsNoOrder(
                "/flowDynamicWithScatterGather/processors/0",
                "/flowDynamicWithScatterGather/processors/0/0/0",
                "/flowDynamicWithScatterGather/processors/0/1/0",
                "/flow/processors/0"));
    }

    @Test
    public void subFlowDynamicWithScatterGather() throws Exception
    {
        muleContext.getClient().send("vm://in-subFlowDynamicWithScatterGather", new DefaultMuleMessage("payload", muleContext));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, not(nullValue()));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, hasExecutedProcessorsNoOrder(
                "/subFlowDynamicWithScatterGather/processors/0",
                "/subFlowDynamicWithScatterGather/processors/0/0/0",
                "/subFlowDynamicWithScatterGather/processors/0/1/0",
                "/subFlowDynamicWithScatterGather/processors/0/1/0/subFlow/subprocessors/0"));
    }

    @Test
    public void flowStaticWithScatterGatherChain() throws Exception
    {
        muleContext.getClient().send("vm://in-flowStaticWithScatterGatherChain", new DefaultMuleMessage("payload", muleContext));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, not(nullValue()));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, hasExecutedProcessorsNoOrder(
                "/flowStaticWithScatterGatherChain/processors/0",
                "/flowStaticWithScatterGatherChain/processors/0/0/0",
                "/flowStaticWithScatterGatherChain/processors/0/1",
                "/flowStaticWithScatterGatherChain/processors/0/1/0",
                "/flow/processors/0"));
    }

    @Ignore("MULE-6926: flaky test")
    @Test
    public void subFlowStaticWithScatterGatherChain() throws Exception
    {
        muleContext.getClient().send("vm://in-subFlowStaticWithScatterGatherChain", new DefaultMuleMessage("payload", muleContext));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, not(nullValue()));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, hasExecutedProcessorsNoOrder(
                "/subFlowStaticWithScatterGatherChain/processors/0",
                "/subFlowStaticWithScatterGatherChain/processors/0/0/0",
                "/subFlowStaticWithScatterGatherChain/processors/0/1",
                "/subFlowStaticWithScatterGatherChain/processors/0/1/0",
                "/subFlowStaticWithScatterGatherChain/processors/0/1/0/subFlow/subprocessors/0"));
    }

    @Test
    public void flowDynamicWithScatterGatherChain() throws Exception
    {
        muleContext.getClient().send("vm://in-flowDynamicWithScatterGatherChain", new DefaultMuleMessage("payload", muleContext));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, not(nullValue()));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, hasExecutedProcessorsNoOrder(
                "/flowDynamicWithScatterGatherChain/processors/0",
                "/flowDynamicWithScatterGatherChain/processors/0/0/0",
                "/flowDynamicWithScatterGatherChain/processors/0/1",
                "/flowDynamicWithScatterGatherChain/processors/0/1/0",
                "/flow/processors/0"));
    }

    @Test
    public void subFlowDynamicWithScatterGatherChain() throws Exception
    {
        muleContext.getClient().send("vm://in-subFlowDynamicWithScatterGatherChain", new DefaultMuleMessage("payload", muleContext));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, not(nullValue()));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, hasExecutedProcessorsNoOrder(
                "/subFlowDynamicWithScatterGatherChain/processors/0",
                "/subFlowDynamicWithScatterGatherChain/processors/0/0/0",
                "/subFlowDynamicWithScatterGatherChain/processors/0/1",
                "/subFlowDynamicWithScatterGatherChain/processors/0/1/0",
                "/subFlowDynamicWithScatterGatherChain/processors/0/1/0/subFlow/subprocessors/0"));
    }

    private abstract class ProcessorsMatcher extends TypeSafeMatcher<ProcessorsTrace>
    {
        protected List<Matcher> failed = new ArrayList<>();
        protected String[] expectedProcessors;

        public ProcessorsMatcher(String[] expectedProcessors)
        {
            this.expectedProcessors = expectedProcessors;
        }

        @Override
        protected boolean matchesSafely(ProcessorsTrace processorsTrace)
        {
            Matcher<Collection<? extends Object>> sizeMatcher = hasSize(expectedProcessors.length);
            if (!sizeMatcher.matches(processorsTrace.getExecutedProcessors()))
            {
                failed.add(sizeMatcher);
            }

            int i = 0;
            for (String expectedProcessor : expectedProcessors)
            {
                doMatch(processorsTrace, i, expectedProcessor);
                ++i;
            }

            return failed.isEmpty();
        }

        protected abstract void doMatch(ProcessorsTrace processorsTrace, int i, String expectedProcessor);

        @Override
        public void describeTo(Description description)
        {
            description.appendValue(Arrays.asList(expectedProcessors));
        }

        @Override
        protected void describeMismatchSafely(ProcessorsTrace item, Description description)
        {
            description.appendText("was ").appendValue(item.getExecutedProcessors());
        }
    }

    private Matcher<ProcessorsTrace> hasExecutedProcessors(final String... expectedProcessors)
    {
        return new ProcessorsMatcher(expectedProcessors)
        {
            @Override
            protected void doMatch(ProcessorsTrace processorsTrace, int i, String expectedProcessor)
            {
                Matcher processorItemMatcher = startsWith(expectedProcessor + " @");
                if (!processorItemMatcher.matches(processorsTrace.getExecutedProcessors().get(i)))
                {
                    failed.add(processorItemMatcher);
                }
            }
        };
    }

    private Matcher<ProcessorsTrace> hasExecutedProcessorsNoOrder(final String... expectedProcessors)
    {
        return new ProcessorsMatcher(expectedProcessors)
        {
            @Override
            protected void doMatch(ProcessorsTrace processorsTrace, int i, String expectedProcessor)
            {
                Matcher<Iterable<? super String>> processorItemMatcher = hasItem(startsWith(expectedProcessor + " @"));
                if (!processorItemMatcher.matches(processorsTrace.getExecutedProcessors()))
                {
                    failed.add(processorItemMatcher);
                }
            }
        };
    }
}
