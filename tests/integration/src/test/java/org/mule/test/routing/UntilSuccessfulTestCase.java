/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mule.tck.functional.InvocationCountMessageProcessor.getNumberOfInvocationsFor;

import org.mule.api.ExceptionPayload;
import org.mule.api.MuleEvent;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.api.client.MuleClient;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.RoutingException;
import org.mule.construct.Flow;
import org.mule.retry.RetryPolicyExhaustedException;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.util.store.AbstractPartitionedObjectStore;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class UntilSuccessfulTestCase extends FunctionalTestCase
{
    private final String configFile;

    private MuleClient client;
    private FunctionalTestComponent targetMessageProcessor;
    private FunctionalTestComponent deadLetterQueueProcessor;

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {"until-successful-test.xml"},
                {"until-successful-seconds-test.xml"}
        });
    }

    public UntilSuccessfulTestCase(String configFile)
    {
        this.configFile = configFile;
    }

    @Override
    protected String getConfigFile()
    {
        return configFile;
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        client = muleContext.getClient();

        targetMessageProcessor = getFunctionalTestComponent("target-mp");
        deadLetterQueueProcessor = getFunctionalTestComponent("dlq-processor");

        final AbstractPartitionedObjectStore<Serializable> objectStore = muleContext.getRegistry()
            .lookupObject("objectStore");
        objectStore.clear("DEFAULT_PARTITION");
    }

    @Test
    public void testDefaultConfiguration() throws Exception
    {
        final String payload = RandomStringUtils.randomAlphanumeric(20);
        client.dispatch("vm://input-1", payload, null);

        final List<Object> receivedPayloads = ponderUntilMessageCountReceivedByTargetMessageProcessor(1);
        assertEquals(1, receivedPayloads.size());
        assertEquals(payload, receivedPayloads.get(0));
    }

    @Test
    public void testFullConfiguration() throws Exception
    {
        final AtomicReference<ExceptionPayload> dlqExceptionPayload = new AtomicReference<>();
        deadLetterQueueProcessor.setEventCallback(new EventCallback()
        {
            @Override
            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {
                dlqExceptionPayload.set(context.getMessage().getExceptionPayload());
            }
        });

        final String payload = RandomStringUtils.randomAlphanumeric(20);
        final MuleMessage response = client.send("vm://input-2", payload, null);
        assertEquals("ACK", response.getPayloadAsString());

        List<Object> receivedPayloads = ponderUntilMessageCountReceivedByTargetMessageProcessor(3);
        assertEquals(3, receivedPayloads.size());
        for (int i = 0; i <= 2; i++)
        {
            assertEquals(payload, receivedPayloads.get(i));
        }

        receivedPayloads = ponderUntilMessageCountReceivedByDlqProcessor(1);
        assertEquals(1, receivedPayloads.size());
        assertEquals(payload, receivedPayloads.get(0));

        assertThat(dlqExceptionPayload.get(), is(notNullValue()));
        assertThat(dlqExceptionPayload.get().getException(), instanceOf(RetryPolicyExhaustedException.class));
        assertThat(dlqExceptionPayload.get().getException().getMessage(),
                containsString("until-successful retries exhausted. Last exception message was: Failure expression positive when processing event"));

        assertThat(dlqExceptionPayload.get().getException().getCause(), instanceOf(MuleRuntimeException.class));
        assertThat(dlqExceptionPayload.get().getException().getMessage(),
                containsString("Failure expression positive when processing event"));
    }

    @Test
    public void testFullConfigurationMP() throws Exception
    {
        final String payload = RandomStringUtils.randomAlphanumeric(20);
        final MuleMessage response = client.send("vm://input-2MP", payload, null);
        assertEquals("ACK", response.getPayloadAsString());

        final List<Object> receivedPayloads = ponderUntilMessageCountReceivedByTargetMessageProcessor(3);
        assertEquals(3, receivedPayloads.size());
        for (int i = 0; i <= 2; i++)
        {
            assertEquals(payload, receivedPayloads.get(i));
        }

        ponderUntilMessageCountReceivedByCustomMP(1);

        ExceptionPayload dlqExceptionPayload = CustomMP.getProcessedMessages().get(0).getExceptionPayload();
        assertThat(dlqExceptionPayload, is(notNullValue()));
        assertThat(dlqExceptionPayload.getException(), instanceOf(RetryPolicyExhaustedException.class));
        assertThat(dlqExceptionPayload.getException().getMessage(),
                containsString("until-successful retries exhausted. Last exception message was: Failure expression positive when processing event"));

        assertThat(dlqExceptionPayload.getException().getCause(), instanceOf(MuleRuntimeException.class));
        assertThat(dlqExceptionPayload.getException().getMessage(),
                containsString("Failure expression positive when processing event"));
    }

    @Test
    public void testRetryOnEndpoint() throws Exception
    {
        final String payload = RandomStringUtils.randomAlphanumeric(20);
        client.dispatch("vm://input-3", payload, null);

        final List<Object> receivedPayloads = ponderUntilMessageCountReceivedByTargetMessageProcessor(3);
        assertEquals(3, receivedPayloads.size());
        for (int i = 0; i <= 2; i++)
        {
            assertEquals(payload, receivedPayloads.get(i));
        }
    }

    @Test(expected = RoutingException.class)
    public void executeSynchronously() throws Exception
    {
        final String payload = RandomStringUtils.randomAlphanumeric(20);
        Flow flow = (Flow) getFlowConstruct("synchronous");
        flow.process(getTestEvent(payload));
        fail("Exception should be thrown");
    }

    @Test
    public void executeSynchronouslyDoingRetries() throws Exception
    {
        try
        {
            final String payload = RandomStringUtils.randomAlphanumeric(20);
            Flow flow = (Flow) getFlowConstruct("synchronous-with-retry");
            flow.process(getTestEvent(payload));
            fail("Exception should be thrown");
        }
        catch (Exception e)
        {
            assertThat(getNumberOfInvocationsFor("untilSuccessful"), is(4));
            assertThat(getNumberOfInvocationsFor("exceptionStrategy"), is(1));
        }
    }

    /**
     * Verifies that the synchronous wait time is consistent with that requested
     */
    @Test
    public void measureSynchronousWait() throws Exception {
        final String payload = RandomStringUtils.randomAlphanumeric(20);
        Flow flow = (Flow) getFlowConstruct("measureSynchronousWait");
        try
        {
            flow.process(getTestEvent(payload));
            fail("Exception should be thrown");
        }
        catch (Exception e)
        {
            assertThat(WaitMeasure.totalWait >= 1000, is(true));
        }
    }

    @Test
    public void executeAsynchronouslyDoingRetries() throws Exception
    {
        final String payload = RandomStringUtils.randomAlphanumeric(20);
        final int expectedCounterExecutions = 4;
        final int expectedCounterInExceptionStrategyExecutions = 1;
        Flow flow = (Flow) getFlowConstruct("asynchronous-using-threading-profile");
        flow.process(getTestEvent(payload));
        new PollingProber(10000, 100).check(new Probe()
        {
            private int executionOfCountInUntilSuccessful;
            private int executionOfCountInExceptionStrategy;

            @Override
            public boolean isSatisfied()
            {
                executionOfCountInUntilSuccessful = getNumberOfInvocationsFor("untilSuccessful2");
                executionOfCountInExceptionStrategy = getNumberOfInvocationsFor("exceptionStrategy2");
                return executionOfCountInUntilSuccessful == expectedCounterExecutions && executionOfCountInExceptionStrategy == expectedCounterInExceptionStrategyExecutions;
            }

            @Override
            public String describeFailure()
            {
                return String.format("Expecting %d executions of counter in until-successful and got %d \n " +
                                     "Expecting %d execution of counter in exception strategy and got %d",
                                     expectedCounterExecutions, executionOfCountInUntilSuccessful, expectedCounterInExceptionStrategyExecutions, executionOfCountInExceptionStrategy);
            }
        });
    }

    @Test
    public void executeAsynchronouslyDoingRetriesAfterRestart() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("asynchronous-using-threading-profile");
        flow.stop();
        flow.start();
        executeAsynchronouslyDoingRetries();

    }

    private List<Object> ponderUntilMessageCountReceivedByTargetMessageProcessor(final int expectedCount)
        throws InterruptedException
    {
        return ponderUntilMessageCountReceived(expectedCount, targetMessageProcessor);
    }

    private List<Object> ponderUntilMessageCountReceivedByDlqProcessor(final int expectedCount)
        throws InterruptedException
    {
        return ponderUntilMessageCountReceived(expectedCount, deadLetterQueueProcessor);
    }

    private List<Object> ponderUntilMessageCountReceived(final int expectedCount,
                                                         final FunctionalTestComponent ftc)
        throws InterruptedException
    {
        final List<Object> results = new ArrayList<Object>();

        while (ftc.getReceivedMessagesCount() < expectedCount)
        {
            Thread.yield();
            Thread.sleep(100L);
        }

        for (int i = 0; i < ftc.getReceivedMessagesCount(); i++)
        {
            results.add(ftc.getReceivedMessage(1 + i));
        }
        return results;
    }

    private void ponderUntilMessageCountReceivedByCustomMP(final int expectedCount)
        throws InterruptedException
    {
        while (CustomMP.getCount() < expectedCount)
        {
            Thread.yield();
            Thread.sleep(100L);
        }
    }

    static class CustomMP implements MessageProcessor
    {
        private static List<MuleMessage> processedMessages = new ArrayList<>();

        public static void clearCount()
        {
            processedMessages.clear();
        }

        public static int getCount()
        {
            return processedMessages.size();
        }

        public static List<MuleMessage> getProcessedMessages()
        {
            return processedMessages;
        }

        @Override
        public MuleEvent process(final MuleEvent event) throws MuleException
        {
            processedMessages.add(event.getMessage());
            return null;
        }
    }

    static class WaitMeasure implements MessageProcessor {

        public static long totalWait;
        private long firstAttemptTime = 0;

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            if (firstAttemptTime == 0) {
                firstAttemptTime = System.currentTimeMillis();
            } else {
                totalWait = System.currentTimeMillis() - firstAttemptTime;
            }

            return event;
        }
    }
}
