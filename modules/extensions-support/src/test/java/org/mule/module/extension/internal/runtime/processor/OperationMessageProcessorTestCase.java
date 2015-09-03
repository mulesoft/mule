/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.processor;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.extension.introspection.ConfigurationModel;
import org.mule.extension.introspection.ExtensionModel;
import org.mule.extension.introspection.OperationModel;
import org.mule.extension.runtime.OperationContext;
import org.mule.extension.runtime.OperationExecutor;
import org.mule.module.extension.internal.config.DeclaredConfiguration;
import org.mule.module.extension.internal.manager.ExtensionManagerAdapter;
import org.mule.module.extension.internal.runtime.OperationContextAdapter;
import org.mule.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
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
    private ExtensionManagerAdapter extensionManager;

    @Mock(extraInterfaces = {Lifecycle.class, MuleContextAware.class})
    private OperationExecutor operationExecutor;

    @Mock
    private MuleContext muleContext;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private ResolverSet resolverSet;

    @Mock
    private ResolverSetResult parameters;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private MuleEvent event;

    @Mock
    private Object configuration;


    private OperationMessageProcessor messageProcessor;
    private String configurationName = CONFIG_NAME;

    @Before
    public void before() throws Exception
    {
        when(operationModel.getExecutor()).thenReturn(operationExecutor);
        when(resolverSet.resolve(event)).thenReturn(parameters);
        when(extensionManager.getConfiguration(same(extensionModel), same(configurationName), any(OperationContext.class))).thenReturn(
                new DeclaredConfiguration<>(CONFIG_NAME, configurationModel, configuration));

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
        assertThat(operationContextAdapter.getOperationModel(), is(sameInstance(operationModel)));
        assertThat(operationContextAdapter.getConfiguration(), is(sameInstance(configuration)));
    }

    @Test
    public void operationReturnsMuleEvent() throws Exception
    {
        MuleEvent responseEvent = mock(MuleEvent.class);
        when(operationExecutor.execute(any(OperationContext.class))).thenReturn(responseEvent);

        assertThat(messageProcessor.process(event), is(sameInstance(responseEvent)));
    }

    @Test
    public void operationReturnsMuleMessage() throws Exception
    {
        MuleMessage responseMessage = mock(MuleMessage.class);
        when(operationExecutor.execute(any(OperationContext.class))).thenReturn(responseMessage);


        assertThat(messageProcessor.process(event), is(sameInstance(event)));
        verify(event).setMessage(responseMessage);
    }

    @Test
    public void operationReturnsPayloadValue() throws Exception
    {
        Object value = new Object();
        when(operationExecutor.execute(any(OperationContext.class))).thenReturn(value);

        assertThat(messageProcessor.process(event), is(sameInstance(event)));
        verify(event.getMessage()).setPayload(value);
    }

    @Test
    public void executesWithDefaultConfig() throws Exception
    {
        configurationName = null;
        messageProcessor = createOperationMessageProcessor();

        Object defaultConfigInstance = new Object();
        DeclaredConfiguration<Object> implicitConfiguration = new DeclaredConfiguration<>(CONFIG_NAME, configurationModel, defaultConfigInstance);
        when(extensionManager.getConfiguration(same(extensionModel), any(OperationContext.class))).thenReturn(implicitConfiguration);

        ArgumentCaptor<OperationContext> operationContextCaptor = ArgumentCaptor.forClass(OperationContext.class);
        messageProcessor.process(event);
        verify(operationExecutor).execute(operationContextCaptor.capture());

        OperationContext operationContext = operationContextCaptor.getValue();

        assertThat(operationContext, is(instanceOf(OperationContextAdapter.class)));
        assertThat(operationContext.getConfiguration(), is(sameInstance(defaultConfigInstance)));
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
}
