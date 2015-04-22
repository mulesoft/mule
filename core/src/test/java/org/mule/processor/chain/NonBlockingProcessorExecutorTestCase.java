package org.mule.processor.chain;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isNotNull;
import static org.mockito.Mockito.when;
import org.mule.MessageExchangePattern;
import org.mule.NonBlockingVoidMuleEvent;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.ProcessorExecutor;
import org.mule.api.transport.NonBlockingResponseReplyToHandler;
import org.mule.processor.BlockingProcessorExecutor;
import org.mule.processor.NonBlockingProcessorExecutor;
import org.mule.tck.SensingNullCompletionHandler;

import java.util.concurrent.TimeUnit;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class NonBlockingProcessorExecutorTestCase extends BlockingProcessorExecutorTestCase
{

    private static final int LATCH_TIMEOUT = 50;

    private SensingNullCompletionHandler completionHandler = new SensingNullCompletionHandler();

    @Override
    public void before() throws MessagingException
    {
        super.before();
        when(event.getReplyToHandler()).thenReturn(new NonBlockingResponseReplyToHandler(completionHandler));
    }

    @Test
    @Override
    public void processNonBlocking() throws MuleException, InterruptedException
    {
        when(event.isAllowNonBlocking()).thenReturn(true);

        assertNonBlocking();
    }

    @Override
    protected BlockingProcessorExecutor createProcessorExecutor()
    {
        return new NonBlockingProcessorExecutor(event, processors, executionTemplate, true);
    }

    private void assertNonBlocking() throws MuleException, InterruptedException
    {
        ProcessorExecutor executor = createProcessorExecutor();

        if (event.getExchangePattern() == MessageExchangePattern.REQUEST_RESPONSE)
        {
            assertThat(executor.execute(), CoreMatchers.<MuleEvent>equalTo(NonBlockingVoidMuleEvent.getInstance()));
        }
        else
        {
            assertThat(executor.execute(), equalTo(event));
        }

        completionHandler.latch.await(LATCH_TIMEOUT, TimeUnit.MILLISECONDS);

        assertThat(mp1.event, is(notNullValue()));
        assertThat(mp1.thread, equalTo(Thread.currentThread()));

        assertThat(mp2.event, is(notNullValue()));
        assertThat(mp2.thread, not(equalTo(mp1.thread)));

        assertThat(mp3.event, is(notNullValue()));
        assertThat(mp2.thread, not(equalTo(mp1.thread)));

        assertThat(completionHandler.event.getMessageAsString(), equalTo(RESULT));
    }

}