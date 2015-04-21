package org.mule.processor.chain;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.*;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.ProcessorExecutor;
import org.mule.execution.MessageProcessorExecutionTemplate;
import org.mule.processor.BlockingProcessorExecutor;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class BlockingProcessorExecutorTestCase extends AbstractMuleTestCase
{

    @Mock(answer = RETURNS_DEEP_STUBS)
    protected MuleContext muleContext;

    @Mock
    protected MuleEvent event;

    @Mock
    protected MessageProcessorExecutionTemplate executionTemplate;

    protected static String A = "a";
    protected static String B = "b";
    protected static String C = "c";
    protected static String RESULT = A + B + C;

    protected SensingNullMessageProcessor mp1 = new SensingNullMessageProcessor(A);
    protected SensingNullMessageProcessor mp2 = new SensingNullMessageProcessor(B);
    protected SensingNullMessageProcessor mp3 = new SensingNullMessageProcessor(C);
    protected List<MessageProcessor> processors = new ArrayList<>();

    @Before
    public void before() throws MessagingException
    {
        processors.add(mp1);
        processors.add(mp2);
        processors.add(mp3);

        when(event.getExchangePattern()).thenReturn(MessageExchangePattern.REQUEST_RESPONSE);
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        when(event.getId()).thenReturn(RandomStringUtils.randomNumeric(3));
        when(event.getMessage()).thenReturn(message);
        when(event.getMuleContext()).thenReturn(muleContext);
        when(executionTemplate.execute(any(MessageProcessor.class), any(MuleEvent.class)))
                .thenAnswer(new Answer<MuleEvent>()
                {
                    @Override
                    public MuleEvent answer(InvocationOnMock invocation) throws Throwable
                    {
                        return ((MessageProcessor) invocation.getArguments()[0]).process((MuleEvent) invocation
                                .getArguments()[1]);
                    }
                });
    }

    @Test
    public void executeRequestResponse() throws MuleException
    {
        when(event.getExchangePattern()).thenReturn(MessageExchangePattern.REQUEST_RESPONSE);
        when(event.isSynchronous()).thenReturn(true);
        assertBlocking();
    }

    @Test
    public void executeRequestResponseNonBlocking() throws MuleException, InterruptedException
    {
        when(event.getExchangePattern()).thenReturn(MessageExchangePattern.REQUEST_RESPONSE);
        when(event.isSynchronous()).thenReturn(false);
        when(event.isAllowNonBlocking()).thenReturn(true);
        assertBlocking();
    }

    @Test
    public void executeOneWay() throws MuleException
    {
        when(event.getExchangePattern()).thenReturn(MessageExchangePattern.ONE_WAY);
        when(event.isSynchronous()).thenReturn(false);
        assertBlocking();
    }

    protected void assertBlocking() throws MuleException
    {
        ProcessorExecutor executor = createProcessorExecutor();

        if (event.getExchangePattern() == MessageExchangePattern.REQUEST_RESPONSE)
        {
            assertThat(executor.execute().getMessageAsString(), equalTo(RESULT));
        }
        else
        {
            assertThat(executor.execute(), equalTo(event));
        }

        assertThat(mp1.event, is(notNullValue()));
        assertThat(mp1.thread, equalTo(Thread.currentThread()));

        assertThat(mp2.event, is(notNullValue()));
        assertThat(mp2.thread, equalTo(Thread.currentThread()));

        assertThat(mp3.event, is(notNullValue()));
        assertThat(mp3.thread, equalTo(Thread.currentThread()));
    }

    protected ProcessorExecutor createProcessorExecutor()
    {
        return new BlockingProcessorExecutor(event, processors, executionTemplate, true);
    }
}