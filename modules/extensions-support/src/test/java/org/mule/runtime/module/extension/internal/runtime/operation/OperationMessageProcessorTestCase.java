/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static java.util.Collections.emptySet;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import static org.mule.runtime.core.message.NullAttributes.NULL_ATTRIBUTES;
import static org.mule.runtime.core.util.SystemUtils.getDefaultEncoding;
import static org.mule.runtime.module.extension.internal.util.ExtensionsTestUtils.toMetadataType;
import org.mule.runtime.api.message.Attributes;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.extension.api.introspection.ImmutableOutputModel;
import org.mule.runtime.extension.api.runtime.operation.OperationContext;
import org.mule.runtime.extension.api.runtime.operation.OperationResult;
import org.mule.runtime.module.extension.internal.runtime.OperationContextAdapter;
import org.mule.tck.size.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class OperationMessageProcessorTestCase extends AbstractOperationMessageProcessorTestCase {

  @Override
  protected OperationMessageProcessor createOperationMessageProcessor() {
    return new OperationMessageProcessor(extensionModel, operationModel, configurationName, target, resolverSet,
                                         extensionManager);
  }

  @Test
  public void operationContextIsWellFormed() throws Exception {
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
  public void operationExecutorIsInvoked() throws Exception {
    messageProcessor.process(event);
    verify(operationExecutor).execute(any(OperationContext.class));
  }

  @Test
  public void operationReturnsOperationResultWhichKeepsNoValues() throws Exception {
    Object payload = new Object();
    MediaType mediaType = ANY.withCharset(getDefaultEncoding(context));
    Attributes attributes = mock(Attributes.class);

    when(operationExecutor.execute(any(OperationContext.class)))
        .thenReturn(OperationResult.builder().output(payload).mediaType(mediaType).attributes(attributes).build());

    ArgumentCaptor<MuleMessage> captor = ArgumentCaptor.forClass(MuleMessage.class);

    messageProcessor.process(event);

    verify(event).setMessage(captor.capture());
    MuleMessage message = captor.getValue();
    assertThat(message, is(notNullValue()));

    assertThat(message.getPayload(), is(sameInstance(payload)));
    assertThat(message.getAttributes(), is(sameInstance(attributes)));
    assertThat(message.getDataType().getMediaType(), is(mediaType));
  }

  @Test
  public void operationReturnsOperationResultOnTarget() throws Exception {
    target = TARGET_VAR;
    messageProcessor = setUpOperationMessageProcessor();

    Object payload = new Object();
    MediaType mediaType = ANY.withCharset(getDefaultEncoding(context));
    Attributes attributes = mock(Attributes.class);

    when(operationExecutor.execute(any(OperationContext.class)))
        .thenReturn(OperationResult.builder().output(payload).mediaType(mediaType).attributes(attributes).build());

    messageProcessor.process(event);

    verify(event, never()).setMessage(any(MuleMessage.class));

    ArgumentCaptor<MuleMessage> captor = ArgumentCaptor.forClass(MuleMessage.class);
    verify(event).setFlowVariable(same(TARGET_VAR), captor.capture());
    MuleMessage message = captor.getValue();
    assertThat(message, is(notNullValue()));

    assertThat(message.getPayload(), is(sameInstance(payload)));
    assertThat(message.getAttributes(), is(sameInstance(attributes)));
    assertThat(message.getDataType().getMediaType(), equalTo(mediaType));
  }

  @Test
  public void operationReturnsOperationResultButKeepsAttributes() throws Exception {
    Object payload = new Object();
    MediaType mediaType = ANY.withCharset(getDefaultEncoding(context));

    when(operationExecutor.execute(any(OperationContext.class)))
        .thenReturn(OperationResult.builder().output(payload).mediaType(mediaType).build());

    when(event.getMessage()).thenReturn(MuleMessage.builder().payload("").attributes(mock(Attributes.class)).build());
    ArgumentCaptor<MuleMessage> captor = ArgumentCaptor.forClass(MuleMessage.class);

    messageProcessor.process(event);

    verify(event).setMessage(captor.capture());
    MuleMessage message = captor.getValue();
    assertThat(message, is(notNullValue()));

    assertThat(message.getPayload(), is(sameInstance(payload)));
    assertThat(message.getAttributes(), is(NULL_ATTRIBUTES));
    assertThat(message.getDataType().getMediaType(), equalTo(mediaType));
  }

  @Test
  public void operationReturnsOperationResultThatOnlySpecifiesPayload() throws Exception {
    Object payload = "hello world!";

    when(operationExecutor.execute(any(OperationContext.class))).thenReturn(OperationResult.builder().output(payload).build());
    when(event.getMessage()).thenReturn(MuleMessage.builder().payload("").attributes(mock(Attributes.class)).build());
    ArgumentCaptor<MuleMessage> captor = ArgumentCaptor.forClass(MuleMessage.class);

    messageProcessor.process(event);

    verify(event).setMessage(captor.capture());
    MuleMessage message = captor.getValue();
    assertThat(message, is(notNullValue()));

    assertThat(message.getPayload(), is(sameInstance(payload)));
    assertThat(message.getAttributes(), is(NULL_ATTRIBUTES));
    assertThat(message.getDataType().getType().equals(String.class), is(true));
  }

  @Test
  public void operationReturnsOperationResultWithPayloadAndAttributes() throws Exception {
    Object payload = "hello world!";
    Attributes attributes = mock(Attributes.class);

    when(operationExecutor.execute(any(OperationContext.class)))
        .thenReturn(OperationResult.builder().output(payload).attributes(attributes).build());
    ArgumentCaptor<MuleMessage> captor = ArgumentCaptor.forClass(MuleMessage.class);

    messageProcessor.process(event);

    verify(event).setMessage(captor.capture());
    MuleMessage message = captor.getValue();
    assertThat(message, is(notNullValue()));

    assertThat(message.getPayload(), is(sameInstance(payload)));
    assertThat(message.getAttributes(), is(sameInstance(attributes)));
    assertThat(message.getDataType().getType().equals(String.class), is(true));
  }

  @Test
  public void operationReturnsPayloadValue() throws Exception {
    Object value = new Object();
    when(operationExecutor.execute(any(OperationContext.class))).thenReturn(value);

    messageProcessor.process(event);

    ArgumentCaptor<MuleMessage> captor = ArgumentCaptor.forClass(MuleMessage.class);
    verify(event).setMessage(captor.capture());

    MuleMessage message = captor.getValue();
    assertThat(message, is(notNullValue()));
    assertThat(message.getPayload(), is(sameInstance(value)));
  }

  @Test
  public void operationReturnsPayloadValueWithTarget() throws Exception {
    target = TARGET_VAR;
    messageProcessor = setUpOperationMessageProcessor();

    Object value = new Object();
    when(operationExecutor.execute(any(OperationContext.class))).thenReturn(value);

    messageProcessor.process(event);

    verify(event, never()).setMessage(any(MuleMessage.class));

    ArgumentCaptor<MuleMessage> captor = ArgumentCaptor.forClass(MuleMessage.class);
    verify(event).setFlowVariable(same(TARGET_VAR), captor.capture());

    MuleMessage message = captor.getValue();
    assertThat(message, is(notNullValue()));
    assertThat(message.getPayload(), is(sameInstance(value)));
  }

  @Test
  public void operationIsVoid() throws Exception {
    when(operationModel.getOutput())
        .thenReturn(new ImmutableOutputModel("MuleMessage.Payload", toMetadataType(void.class), false, emptySet()));
    messageProcessor = setUpOperationMessageProcessor();

    when(operationExecutor.execute(any(OperationContext.class))).thenReturn(null);
    assertThat(messageProcessor.process(event), is(sameInstance(event)));
    verify(event, never()).setMessage(any(MuleMessage.class));
  }

  @Test
  public void executesWithDefaultConfig() throws Exception {
    configurationName = null;
    messageProcessor = setUpOperationMessageProcessor();

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
}
