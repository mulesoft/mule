/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.processor;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.metadata.DataType;
import org.mule.api.temporary.MuleMessage;
import org.mule.extension.api.ExtensionManager;
import org.mule.extension.api.introspection.ConfigurationModel;
import org.mule.extension.api.introspection.ExceptionEnricherFactory;
import org.mule.extension.api.introspection.ExtensionModel;
import org.mule.extension.api.introspection.OperationModel;
import org.mule.extension.api.runtime.ConfigurationInstance;
import org.mule.extension.api.runtime.OperationContext;
import org.mule.extension.api.runtime.OperationExecutor;
import org.mule.extension.api.runtime.OperationExecutorFactory;
import org.mule.module.extension.internal.runtime.OperationContextAdapter;
import org.mule.module.extension.internal.runtime.exception.NullExceptionEnricher;
import org.mule.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class OperationMessageProcessorTestCase extends AbstractMuleTestCase
{

    private static final String CONFIG_NAME = "config";

    @Mock
    private ExtensionModel extensionModel;

    @Mock
    private ConfigurationModel configurationModel;

    @Mock
    private OperationModel operationModel;

    @Mock
    private ExtensionManager extensionManager;

    @Mock
    private OperationExecutorFactory operationExecutorFactory;

    @Mock(extraInterfaces = {Lifecycle.class, MuleContextAware.class})
    private OperationExecutor operationExecutor;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private MuleContext muleContext;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private ResolverSet resolverSet;

    @Mock
    private ResolverSetResult parameters;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private MuleEvent event;

    @Mock
    private ConfigurationInstance<Object> configurationInstance;

    @Mock
    private Object configuration;

    @Mock
    private ExceptionEnricherFactory exceptionEnricherFactory;

    private OperationMessageProcessor messageProcessor;
    private String configurationName = CONFIG_NAME;

    @Before
    public void before() throws Exception
    {
        configureMockEvent(event);

        when(operationModel.getReturnType()).thenReturn(org.mule.extension.api.introspection.DataType.of(String.class));
        when(operationModel.getExecutor()).thenReturn(operationExecutorFactory);
        when(operationModel.getExceptionEnricherFactory()).thenReturn(Optional.of(exceptionEnricherFactory));
        when(exceptionEnricherFactory.createEnricher()).thenReturn(new NullExceptionEnricher());
        when(operationExecutorFactory.createExecutor()).thenReturn(operationExecutor);
        when(resolverSet.resolve(event)).thenReturn(parameters);

        when(configurationInstance.getName()).thenReturn(CONFIG_NAME);
        when(configurationInstance.getModel()).thenReturn(configurationModel);
        when(configurationInstance.getValue()).thenReturn(configuration);

        when(extensionManager.getConfiguration(CONFIG_NAME, event)).thenReturn(configurationInstance);
        when(extensionManager.getConfiguration(extensionModel, event)).thenReturn(configurationInstance);

        messageProcessor = createOperationMessageProcessor();
    }

    @Test
    public void operationExecutorIsInvoked() throws Exception
    {
        messageProcessor.process(event);
        verify(operationExecutor).execute(any(OperationContext.class));
    }

    @Test
    public void operationContextIsWellFormed() throws Exception
    {
        ArgumentCaptor<OperationContext> operationContextCaptor = ArgumentCaptor.forClass(OperationContext.class);
        messageProcessor.process(event);

        verify(operationExecutor).execute(operationContextCaptor.capture());
        OperationContext operationContext = operationContextCaptor.getValue();

        assertThat(operationContext, is(instanceOf(OperationContextAdapter.class)));
        OperationContextAdapter operationContextAdapter = (OperationContextAdapter) operationContext;

        assertThat(operationContextAdapter.getEvent(), is(sameInstance(event)));
        assertThat(operationContextAdapter.getConfiguration().getValue(), is(sameInstance(configuration)));
    }

    @Test
    public void operationReturnsMuleMessageWichKeepsNoValues() throws Exception
    {
        Object payload = new Object();
        DataType dataType = mock(DataType.class);
        Serializable attributes = mock(Serializable.class);

        when(operationExecutor.execute(any(OperationContext.class))).thenReturn(new DefaultMuleMessage(payload, dataType, attributes));

        ArgumentCaptor<DefaultMuleMessage> captor = ArgumentCaptor.forClass(DefaultMuleMessage.class);

        messageProcessor.process(event);

        verify(event).setMessage(captor.capture());
        MuleMessage message = captor.getValue();
        assertThat(message, is(notNullValue()));

        assertThat(message.getPayload(), is(sameInstance(payload)));
        assertThat(message.getAttributes(), is(sameInstance(attributes)));
        assertThat(message.getDataType(), is(sameInstance(dataType)));
    }

    @Test
    public void operationReturnsMuleMessageButKeepsAttributes() throws Exception
    {
        Object payload = new Object();
        DataType dataType = mock(DataType.class);
        Serializable oldAttributes = mock(Serializable.class);

        when(operationExecutor.execute(any(OperationContext.class))).thenReturn(new DefaultMuleMessage(payload, dataType));
        when(event.getMessage()).thenReturn(new DefaultMuleMessage("", mock(DataType.class), oldAttributes));
        ArgumentCaptor<DefaultMuleMessage> captor = ArgumentCaptor.forClass(DefaultMuleMessage.class);

        messageProcessor.process(event);

        verify(event).setMessage(captor.capture());
        MuleMessage message = captor.getValue();
        assertThat(message, is(notNullValue()));

        assertThat(message.getPayload(), is(sameInstance(payload)));
        assertThat(message.getAttributes(), is(sameInstance(oldAttributes)));
        assertThat(message.getDataType(), is(sameInstance(dataType)));
    }

    @Test
    public void operationReturnsMuleMessageThatOnlySpecifiesPayload() throws Exception
    {
        Object payload = "hello world!";
        Serializable oldAttributes = mock(Serializable.class);

        when(operationExecutor.execute(any(OperationContext.class))).thenReturn(new DefaultMuleMessage(payload));
        when(event.getMessage()).thenReturn(new DefaultMuleMessage("", mock(DataType.class), oldAttributes));
        ArgumentCaptor<DefaultMuleMessage> captor = ArgumentCaptor.forClass(DefaultMuleMessage.class);

        messageProcessor.process(event);

        verify(event).setMessage(captor.capture());
        MuleMessage message = captor.getValue();
        assertThat(message, is(notNullValue()));

        assertThat(message.getPayload(), is(sameInstance(payload)));
        assertThat(message.getAttributes(), is(sameInstance(oldAttributes)));
        assertThat(message.getDataType().getType().equals(String.class), is(true));
    }

    @Test
    public void operationReturnsMuleMessageWithPayloadAndAttributes() throws Exception
    {
        Object payload = "hello world!";
        Serializable attributes = mock(Serializable.class);

        when(operationExecutor.execute(any(OperationContext.class))).thenReturn(new DefaultMuleMessage(payload, attributes));
        ArgumentCaptor<DefaultMuleMessage> captor = ArgumentCaptor.forClass(DefaultMuleMessage.class);

        messageProcessor.process(event);

        verify(event).setMessage(captor.capture());
        MuleMessage message = captor.getValue();
        assertThat(message, is(notNullValue()));

        assertThat(message.getPayload(), is(sameInstance(payload)));
        assertThat(message.getAttributes(), is(sameInstance(attributes)));
        assertThat(message.getDataType().getType().equals(String.class), is(true));
    }

    @Test
    public void operationReturnsPayloadValue() throws Exception
    {
        Object value = new Object();
        when(operationExecutor.execute(any(OperationContext.class))).thenReturn(value);

        messageProcessor.process(event);

        verify(event.getMessage()).setPayload(value);
    }

    @Test
    public void operationIsVoid() throws Exception
    {
        when(operationModel.getReturnType()).thenReturn(org.mule.extension.api.introspection.DataType.of(void.class));
        messageProcessor = createOperationMessageProcessor();

        when(operationExecutor.execute(any(OperationContext.class))).thenReturn(null);
        assertThat(messageProcessor.process(event), is(sameInstance(event)));
        verify(event, never()).setMessage(Mockito.any(org.mule.api.MuleMessage.class));
    }

    @Test
    public void executesWithDefaultConfig() throws Exception
    {
        configurationName = null;
        messageProcessor = createOperationMessageProcessor();

        Object defaultConfigInstance = new Object();
        when(configurationInstance.getValue()).thenReturn(defaultConfigInstance);
        when(extensionManager.getConfiguration(extensionModel, event)).thenReturn(configurationInstance);

        ArgumentCaptor<OperationContext> operationContextCaptor = ArgumentCaptor.forClass(OperationContext.class);
        messageProcessor.process(event);
        verify(operationExecutor).execute(operationContextCaptor.capture());

        OperationContext operationContext = operationContextCaptor.getValue();

        assertThat(operationContext, is(instanceOf(OperationContextAdapter.class)));
        assertThat(operationContext.getConfiguration().getValue(), is(sameInstance(defaultConfigInstance)));
    }

    @Test
    public void initialise() throws Exception
    {
        verify((MuleContextAware) operationExecutor).setMuleContext(muleContext);
        verify((Initialisable) operationExecutor).initialise();
    }

    @Test
    public void start() throws Exception
    {
        messageProcessor.start();
        verify((Startable) operationExecutor).start();
    }

    @Test
    public void stop() throws Exception
    {
        messageProcessor.stop();
        verify((Stoppable) operationExecutor).stop();
    }

    @Test
    public void dispose() throws Exception
    {
        messageProcessor.dispose();
        verify((Disposable) operationExecutor).dispose();
    }

    private OperationMessageProcessor createOperationMessageProcessor() throws Exception
    {
        OperationMessageProcessor messageProcessor = new OperationMessageProcessor(extensionModel, operationModel, configurationName, resolverSet, extensionManager);
        messageProcessor.setMuleContext(muleContext);
        messageProcessor.initialise();
        return messageProcessor;
    }

    private MuleEvent configureMockEvent(MuleEvent mockEvent)
    {
        when(mockEvent.getMessage().getEncoding()).thenReturn(Charset.defaultCharset().name());
        return mockEvent;
    }
}
