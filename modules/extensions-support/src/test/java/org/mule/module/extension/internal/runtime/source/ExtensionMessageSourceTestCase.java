/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.source;

import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.api.config.MuleProperties.OBJECT_EXTENSION_MANAGER;
import static org.mule.tck.MuleTestUtils.spyInjector;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.connection.ConnectionException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.context.WorkManager;
import org.mule.api.execution.CompletionHandler;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.LifecycleUtils;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.execution.MessageProcessingManager;
import org.mule.extension.api.ExtensionManager;
import org.mule.extension.api.introspection.ExtensionModel;
import org.mule.extension.api.runtime.source.Source;
import org.mule.extension.api.runtime.source.SourceContext;
import org.mule.extension.api.runtime.source.SourceFactory;
import org.mule.retry.RetryPolicyExhaustedException;
import org.mule.retry.policies.SimpleRetryPolicyTemplate;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import javax.resource.spi.work.Work;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ExtensionMessageSourceTestCase extends AbstractMuleContextTestCase
{

    private static final String CONFIG_NAME = "myConfig";
    private static final String ERROR_MESSAGE = "ERROR";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private ExtensionModel extensionModel;

    @Mock
    private SourceFactory sourceFactory;

    @Mock
    private ThreadingProfile threadingProfile;

    @Mock
    private WorkManager workManager;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private MessageProcessor messageProcessor;

    @Mock
    private FlowConstruct flowConstruct;

    @Mock(extraInterfaces = Lifecycle.class)
    private Source source;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private ExtensionManager extensionManager;

    @Mock
    private MessageProcessingManager messageProcessingManager;

    @Mock
    private MuleMessage muleMessage;

    private ExtensionMessageSource messageSource;

    public final RetryPolicyTemplate retryPolicyTemplate = new SimpleRetryPolicyTemplate(0, 2);

    @Before
    public void before() throws Exception
    {
        spyInjector(muleContext);
        when(threadingProfile.createWorkManager(anyString(), eq(muleContext.getConfiguration().getShutdownTimeout()))).thenReturn(workManager);
        when(sourceFactory.createSource()).thenReturn(source);
        when(muleMessage.getMuleContext()).thenReturn(muleContext);

        LifecycleUtils.initialiseIfNeeded(retryPolicyTemplate, muleContext);

        messageSource = new ExtensionMessageSource(extensionModel, sourceFactory, CONFIG_NAME, threadingProfile, retryPolicyTemplate);
        messageSource.setListener(messageProcessor);
        messageSource.setFlowConstruct(flowConstruct);

        muleContext.getRegistry().registerObject(OBJECT_EXTENSION_MANAGER, extensionManager);
        muleContext.getInjector().inject(messageSource);
    }

    @Test
    public void handleMessage() throws Exception
    {
        doAnswer(invocation -> {
            ((Work) invocation.getArguments()[0]).run();
            return null;
        }).when(workManager).scheduleWork(any(Work.class));

        messageSource.initialise();
        messageSource.start();

        CompletionHandler completionHandler = mock(CompletionHandler.class);
        messageSource.handle(muleMessage, completionHandler);

        ArgumentCaptor<MuleEvent> eventCaptor = ArgumentCaptor.forClass(MuleEvent.class);
        verify(messageProcessor).process(eventCaptor.capture());

        MuleEvent event = eventCaptor.getValue();
        assertThat(event.getMessage(), is(sameInstance(muleMessage)));
        verify(completionHandler).onCompletion(any(MuleMessage.class));
    }

    @Test
    public void handleExceptionAndRestart() throws Exception
    {
        messageSource.initialise();
        messageSource.start();

        messageSource.onException(new ConnectionException(ERROR_MESSAGE));
        verify((Stoppable) source).stop();
        verify(workManager, never()).dispose();
        verify((Disposable) source).dispose();
        verify((Initialisable) source, times(3)).initialise();
        verify((Startable) source, times(2)).start();
        handleMessage();
    }

    @Test
    public void initialise() throws Exception
    {
        messageSource.initialise();
        verify(source).setSourceContext(any(SourceContext.class));
        verify(muleContext.getInjector()).inject(source);
        verify((Initialisable) source).initialise();
    }

    @Test
    public void initialiseFailsWithInitialisationException() throws Exception
    {
        Exception e = mock(InitialisationException.class);
        doThrow(e).when(((Initialisable) source)).initialise();
        expectedException.expect(is(sameInstance(e)));

        messageSource.initialise();
    }

    @Test
    public void failWithConnectionExceptionWhenStartingAndGetRetryPolicyExhausted() throws Exception
    {
        doThrow(new RuntimeException(new ConnectionException(ERROR_MESSAGE))).when(source).start();

        final Throwable throwable = catchThrowable(messageSource::start);
        assertThat(throwable, is(instanceOf(MuleRuntimeException.class)));
        assertThat(throwable.getCause(), is(instanceOf(RetryPolicyExhaustedException.class)));
        verify(source, times(3)).start();
    }

    @Test
    public void failWithNonConnectionExceptionWhenStartingAndGetRetryPolicyExhausted() throws Exception
    {
        doThrow(new RuntimeException()).when(source).start();

        final Throwable throwable = catchThrowable(messageSource::start);
        assertThat(throwable, is(instanceOf(MuleRuntimeException.class)));
        assertThat(throwable.getCause(), is(instanceOf(RuntimeException.class)));
        verify(source, times(1)).start();
    }

    @Test
    public void failWithConnectionExceptionWhenStartingAndGetsReconnected() throws Exception
    {
        doThrow(new RuntimeException(new ConnectionException(ERROR_MESSAGE)))
                .doThrow(new RuntimeException(new ConnectionException(ERROR_MESSAGE)))
                .doNothing()
                .when(source)
                .start();

        messageSource.initialise();
        messageSource.start();
        verify(source, times(3)).start();
        verify(source, times(2)).stop();
    }

    @Test
    public void failOnExceptionWithConnectionExceptionAndGetsReconnected() throws Exception
    {
        messageSource.initialise();
        messageSource.start();
        messageSource.onException(new ConnectionException(ERROR_MESSAGE));

        verify(source, times(2)).start();
        verify(source, times(1)).stop();
    }

    @Test
    public void failOnExceptionWithNonConnectionExceptionAndGetsExhausted() throws Exception
    {
        initialise();
        messageSource.start();
        messageSource.onException(new RuntimeException(ERROR_MESSAGE));

        verify(source, times(1)).start();
        verify(source, times(1)).stop();
    }

    @Test
    public void initialiseFailsWithRandomException() throws Exception
    {
        Exception e = new RuntimeException();
        doThrow(e).when(((Initialisable) source)).initialise();
        expectedException.expectCause(is(sameInstance(e)));

        messageSource.initialise();
    }

    @Test
    public void start() throws Exception
    {
        initialise();
        messageSource.start();

        verify(workManager).start();
        verify((Startable) source).start();
    }

    @Test
    public void failedToCreateWorkManager() throws Exception
    {
        Exception e = new RuntimeException();
        when(threadingProfile.createWorkManager(anyString(), eq(muleContext.getConfiguration().getShutdownTimeout()))).thenThrow(e);
        initialise();

        final Throwable throwable = catchThrowable(messageSource::start);
        assertThat(throwable, is(sameInstance(e)));
        verify((Startable) source, never()).start();
    }

    @Test
    public void stop() throws Exception
    {
        messageSource.initialise();
        messageSource.start();
        InOrder inOrder = inOrder(source, workManager);

        messageSource.stop();
        inOrder.verify((Stoppable) source).stop();
        inOrder.verify(workManager).dispose();
    }

    @Test
    public void workManagerDisposedIfSourceFailsToStart() throws Exception
    {
        messageSource.initialise();
        messageSource.start();

        Exception e = new RuntimeException();
        doThrow(e).when((Stoppable) source).stop();
        expectedException.expectCause(is(sameInstance(e)));

        messageSource.stop();
        verify(workManager).dispose();
    }

    @Test
    public void dispose() throws Exception
    {
        messageSource.initialise();
        messageSource.start();
        messageSource.stop();
        messageSource.dispose();

        verify((Disposable) source).dispose();
    }
}
