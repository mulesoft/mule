/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.context.notification;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_FLOW_TRACE;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.context.notification.MessageProcessorNotificationListener;
import org.mule.runtime.core.api.context.notification.ProcessorsTrace;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.context.notification.MessageProcessorNotification;
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

public class ProcessorsTraceTestCase extends AbstractIntegrationTestCase
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
        flowRunner("flowStatic").withPayload(TEST_PAYLOAD).run();

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, not(nullValue()));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, hasExecutedProcessors(
                "/flowStatic/processors/0",
                "/flow/processors/0"));
    }

    @Test
    public void subFlowStatic() throws Exception
    {
        flowRunner("subFlowStatic").withPayload(TEST_PAYLOAD).run();

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, not(nullValue()));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, hasExecutedProcessors(
                "/subFlowStatic/processors/0",
                "/subFlowStatic/processors/0/subFlow/subprocessors/0"));
    }

    @Test
    public void flowDynamic() throws Exception
    {
        flowRunner("flowDynamic").withPayload(TEST_PAYLOAD).run();

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, not(nullValue()));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, hasExecutedProcessors(
                "/flowDynamic/processors/0",
                "/flow/processors/0"));
    }

    @Test
    public void subFlowDynamic() throws Exception
    {
        flowRunner("subFlowDynamic").withPayload(TEST_PAYLOAD).run();

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, not(nullValue()));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, hasExecutedProcessors(
                "/subFlowDynamic/processors/0",
                "/subFlowDynamic/processors/0/subFlow/subprocessors/0"));
    }

    @Test
    public void secondFlowStatic() throws Exception
    {
        flowRunner("secondFlowStatic").withPayload(TEST_PAYLOAD).run();

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, not(nullValue()));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, hasExecutedProcessors(
                "/secondFlowStatic/processors/0",
                "/flow/processors/0",
                "/secondFlowStatic/processors/1",
                "/flow/processors/0"));
    }

    @Test
    public void secondSubFlowStatic() throws Exception
    {
        flowRunner("secondSubFlowStatic").withPayload(TEST_PAYLOAD).run();

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
        flowRunner("secondFlowDynamic").withPayload(TEST_PAYLOAD).run();

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, not(nullValue()));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, hasExecutedProcessors(
                "/secondFlowDynamic/processors/0",
                "/flow/processors/0",
                "/secondFlowDynamic/processors/1",
                "/flow/processors/0"));
    }

    @Test
    public void secondSubFlowDynamic() throws Exception
    {
        flowRunner("secondSubFlowDynamic").withPayload(TEST_PAYLOAD).run();

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
        flowRunner("flowStaticWithAsync").withPayload(TEST_PAYLOAD).run();

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
        flowRunner("subFlowStaticWithAsync").withPayload(TEST_PAYLOAD).run();

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
        flowRunner("flowDynamicWithAsync").withPayload(TEST_PAYLOAD).run();

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
        flowRunner("subFlowDynamicWithAsync").withPayload(TEST_PAYLOAD).run();

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
        flowRunner("flowStaticWithEnricher").withPayload(TEST_PAYLOAD).run();

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, not(nullValue()));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, hasExecutedProcessors(
                "/flowStaticWithEnricher/processors/0",
                "/flowStaticWithEnricher/processors/0/0",
                "/flow/processors/0"));
    }

    @Test
    public void subFlowStaticWithEnricher() throws Exception
    {
        flowRunner("subFlowStaticWithEnricher").withPayload(TEST_PAYLOAD).run();

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, not(nullValue()));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, hasExecutedProcessors(
                "/subFlowStaticWithEnricher/processors/0",
                "/subFlowStaticWithEnricher/processors/0/0/subFlow/subprocessors/0"));
    }

    @Test
    public void flowDynamicWithEnricher() throws Exception
    {
        flowRunner("flowDynamicWithEnricher").withPayload(TEST_PAYLOAD).run();

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, not(nullValue()));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, hasExecutedProcessors(
                "/flowDynamicWithEnricher/processors/0",
                "/flowDynamicWithEnricher/processors/0/0",
                "/flow/processors/0"));
    }

    @Test
    public void subFlowDynamicWithEnricher() throws Exception
    {
        flowRunner("subFlowDynamicWithEnricher").withPayload(TEST_PAYLOAD).run();

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, not(nullValue()));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, hasExecutedProcessors(
                "/subFlowDynamicWithEnricher/processors/0",
                "/subFlowDynamicWithEnricher/processors/0/0",
                "/subFlowDynamicWithEnricher/processors/0/0/subFlow/subprocessors/0"));
    }

    @Test
    public void flowStaticWithChoice() throws Exception
    {
        flowRunner("flowStaticWithChoice").withPayload(TEST_PAYLOAD).run();

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, not(nullValue()));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, hasExecutedProcessors(
                "/flowStaticWithChoice/processors/0",
                "/flowStaticWithChoice/processors/0/0/0",
                "/flow/processors/0"));
    }

    @Test
    public void subFlowStaticWithChoice() throws Exception
    {
        flowRunner("subFlowStaticWithChoice").withPayload(TEST_PAYLOAD).run();

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, not(nullValue()));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, hasExecutedProcessors(
                "/subFlowStaticWithChoice/processors/0",
                "/subFlowStaticWithChoice/processors/0/0/0",
                "/subFlowStaticWithChoice/processors/0/0/0/subFlow/subprocessors/0"));
    }

    @Test
    public void flowDynamicWithChoice() throws Exception
    {
        flowRunner("flowDynamicWithChoice").withPayload(TEST_PAYLOAD).run();

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, not(nullValue()));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, hasExecutedProcessors(
                "/flowDynamicWithChoice/processors/0",
                "/flowDynamicWithChoice/processors/0/0/0",
                "/flow/processors/0"));
    }

    @Test
    public void subFlowDynamicWithChoice() throws Exception
    {
        flowRunner("subFlowDynamicWithChoice").withPayload(TEST_PAYLOAD).run();

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, not(nullValue()));

        assertThat(ProcessorsTraceAsserter.processorsTraceToAssert, hasExecutedProcessors(
                "/subFlowDynamicWithChoice/processors/0",
                "/subFlowDynamicWithChoice/processors/0/0/0",
                "/subFlowDynamicWithChoice/processors/0/0/0/subFlow/subprocessors/0"));
    }

    @Test
    public void flowStaticWithScatterGather() throws Exception
    {
        flowRunner("flowStaticWithScatterGather").withPayload(TEST_PAYLOAD).run();

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
        flowRunner("subFlowStaticWithScatterGather").withPayload(TEST_PAYLOAD).run();

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
        flowRunner("flowDynamicWithScatterGather").withPayload(TEST_PAYLOAD).run();

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
        flowRunner("subFlowDynamicWithScatterGather").withPayload(TEST_PAYLOAD).run();

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
        flowRunner("flowStaticWithScatterGatherChain").withPayload(TEST_PAYLOAD).run();

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
        flowRunner("subFlowStaticWithScatterGatherChain").withPayload(TEST_PAYLOAD).run();

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
        flowRunner("flowDynamicWithScatterGatherChain").withPayload(TEST_PAYLOAD).run();

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
        flowRunner("subFlowDynamicWithScatterGatherChain").withPayload(TEST_PAYLOAD).run();

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
