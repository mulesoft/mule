package org.mule.processor.chain;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.api.MuleEvent;
import org.mule.api.processor.ProcessorExecutor;
import org.mule.api.service.Service;
import org.mule.api.transport.NonBlockingResponseReplyToHandler;
import org.mule.construct.Flow;
import org.mule.processor.BlockingProcessorExecutor;
import org.mule.processor.NonBlockingProcessorExecutor;
import org.mule.service.processor.ServiceProcessorExecutor;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ProcessorExecutorFactoryTestCase extends AbstractMuleTestCase
{

    @Mock
    private MuleEvent muleEvent;

    @Test
    public void service()
    {
        when(muleEvent.getFlowConstruct()).thenReturn(mock(Service.class));
        assertThat(createProcessorExecutor().getClass(), CoreMatchers.<Class>equalTo(
                (ServiceProcessorExecutor.class)));
    }

    @Test
    public void flow()
    {
        when(muleEvent.getFlowConstruct()).thenReturn(mock(Flow.class));
        when(muleEvent.isAllowNonBlocking()).thenReturn(false);
        assertThat(createProcessorExecutor().getClass(), CoreMatchers.<Class>equalTo(
                (BlockingProcessorExecutor.class)));
    }

    @Test
    public void flowReplyHandler()
    {
        when(muleEvent.getFlowConstruct()).thenReturn(mock(Flow.class));
        when(muleEvent.isAllowNonBlocking()).thenReturn(false);
        when(muleEvent.getReplyToHandler()).thenReturn(mock(NonBlockingResponseReplyToHandler.class));
        assertThat(createProcessorExecutor().getClass(), CoreMatchers.<Class>equalTo(
                (BlockingProcessorExecutor.class)));
    }

    @Test
    public void flowNonBlockingAllowed()
    {
        when(muleEvent.getFlowConstruct()).thenReturn(mock(Flow.class));
        when(muleEvent.isAllowNonBlocking()).thenReturn(true);
        assertThat(createProcessorExecutor().getClass(), CoreMatchers.<Class>equalTo(
                (BlockingProcessorExecutor.class)));
    }

    @Test
    public void flowNonBlockingAllowedReplyHandler()
    {
        when(muleEvent.getFlowConstruct()).thenReturn(mock(Flow.class));
        when(muleEvent.isAllowNonBlocking()).thenReturn(true);
        when(muleEvent.getReplyToHandler()).thenReturn(mock(NonBlockingResponseReplyToHandler.class));
        assertThat(createProcessorExecutor().getClass(), CoreMatchers.<Class>equalTo(
                (NonBlockingProcessorExecutor.class)));
    }

    private ProcessorExecutor createProcessorExecutor()
    {
        return new ProcessorExecutorFactory().createProcessorExecutor(muleEvent, null, null, false);
    }

}