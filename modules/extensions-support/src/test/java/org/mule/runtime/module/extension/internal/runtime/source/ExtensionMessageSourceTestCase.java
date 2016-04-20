/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.core.StringContains.containsString;
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
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXTENSION_MANAGER;
import static org.mule.test.heisenberg.extension.exception.HeisenbergConnectionExceptionEnricher.ENRICHED_MESSAGE;
import static org.mule.tck.MuleTestUtils.spyInjector;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.config.ThreadingProfile;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.WorkManager;
import org.mule.runtime.api.execution.CompletionHandler;
import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.lifecycle.LifecycleUtils;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.retry.RetryPolicyTemplate;
import org.mule.runtime.core.execution.MessageProcessingManager;
import org.mule.runtime.extension.api.introspection.exception.ExceptionEnricher;
import org.mule.runtime.extension.api.introspection.exception.ExceptionEnricherFactory;
import org.mule.runtime.extension.api.introspection.config.RuntimeConfigurationModel;
import org.mule.runtime.extension.api.introspection.RuntimeExtensionModel;
import org.mule.runtime.extension.api.introspection.source.RuntimeSourceModel;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceContext;
import org.mule.runtime.extension.api.runtime.source.SourceFactory;
import org.mule.test.heisenberg.extension.exception.HeisenbergConnectionExceptionEnricher;
import org.mule.runtime.module.extension.internal.manager.ExtensionManagerAdapter;
import org.mule.runtime.core.retry.RetryPolicyExhaustedException;
import org.mule.runtime.core.retry.policies.SimpleRetryPolicyTemplate;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.runtime.core.util.ExceptionUtils;

import java.nio.charset.Charset;
import java.util.Optional;

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
    private static final String SOURCE_NAME = "source";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private RuntimeExtensionModel extensionModel;

    @Mock
    private RuntimeSourceModel sourceModel;

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
    private ExtensionManagerAdapter extensionManager;

    @Mock
    private MessageProcessingManager messageProcessingManager;

    @Mock
    private ExceptionEnricherFactory enricherFactory;

    @Mock
    private MuleMessage muleMessage;

    @Mock
    private ConfigurationProvider<Object> configurationProvider;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private RuntimeConfigurationModel configurationModel;

    @Mock
    private ConfigurationInstance<Object> configurationInstance;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private MuleEvent event;

    private ExtensionMessageSource messageSource;
    private final RetryPolicyTemplate retryPolicyTemplate = new SimpleRetryPolicyTemplate(0, 2);

    @Before
    public void before() throws Exception
    {
        spyInjector(muleContext);
        when(threadingProfile.createWorkManager(anyString(), eq(muleContext.getConfiguration().getShutdownTimeout()))).thenReturn(workManager);
        when(sourceFactory.createSource()).thenReturn(source);
        when(muleMessage.getMuleContext()).thenReturn(muleContext);
        when(sourceModel.getExceptionEnricherFactory()).thenReturn(Optional.empty());
        when(sourceModel.getName()).thenReturn(SOURCE_NAME);
        when(extensionModel.getExceptionEnricherFactory()).thenReturn(Optional.empty());

        LifecycleUtils.initialiseIfNeeded(retryPolicyTemplate, muleContext);

        muleContext.getRegistry().registerObject(OBJECT_EXTENSION_MANAGER, extensionManager);

        when(configurationModel.getSourceModel(SOURCE_NAME)).thenReturn(Optional.of(sourceModel));
        when(extensionManager.getConfigurationProvider(CONFIG_NAME)).thenReturn(Optional.of(configurationProvider));
        when(configurationProvider.get(configureMockEvent(event))).thenReturn(configurationInstance);
        when(configurationProvider.getModel()).thenReturn(configurationModel);

        messageSource = getNewExtensionMessageSourceInstance();
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
    public void enrichExceptionWithSourceExceptionEnricher() throws Exception
    {
        when(enricherFactory.createEnricher()).thenReturn(new HeisenbergConnectionExceptionEnricher());
        when(sourceModel.getExceptionEnricherFactory()).thenReturn(Optional.of(enricherFactory));
        ExtensionMessageSource messageSource = getNewExtensionMessageSourceInstance();
        doThrow(new RuntimeException(ERROR_MESSAGE)).when(source).start();
        Throwable t = catchThrowable(messageSource::start);

        assertThat(ExceptionUtils.containsType(t, ConnectionException.class), is(true));
        assertThat(t.getMessage(), containsString(ENRICHED_MESSAGE + ERROR_MESSAGE));
    }

    @Test
    public void enrichExceptionWithExtensionEnricher() throws Exception
    {
        final String enrichedErrorMessage = "Enriched: " + ERROR_MESSAGE;
        ExceptionEnricher exceptionEnricher = mock(ExceptionEnricher.class);
        when(exceptionEnricher.enrichException(any(Exception.class))).thenReturn(new Exception(enrichedErrorMessage));
        when(enricherFactory.createEnricher()).thenReturn(exceptionEnricher);
        when(extensionModel.getExceptionEnricherFactory()).thenReturn(Optional.of(enricherFactory));
        ExtensionMessageSource messageSource = getNewExtensionMessageSourceInstance();
        doThrow(new RuntimeException(ERROR_MESSAGE)).when(source).start();
        Throwable t = catchThrowable(messageSource::start);

        assertThat(t.getMessage(), containsString(enrichedErrorMessage));
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

    private ExtensionMessageSource getNewExtensionMessageSourceInstance() throws MuleException
    {
        ExtensionMessageSource messageSource = new ExtensionMessageSource(extensionModel, sourceModel, sourceFactory, CONFIG_NAME, threadingProfile, retryPolicyTemplate, extensionManager);
        messageSource.setListener(messageProcessor);
        messageSource.setFlowConstruct(flowConstruct);
        muleContext.getInjector().inject(messageSource);
        return messageSource;
    }

    private MuleEvent configureMockEvent(MuleEvent mockEvent)
    {
        when(mockEvent.getMessage().getEncoding()).thenReturn(Charset.defaultCharset().name());
        return mockEvent;
    }
}
