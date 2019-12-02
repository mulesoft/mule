/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonMap;
import static java.util.Optional.empty;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_JSON;
import static org.mule.runtime.core.api.util.SystemUtils.getDefaultEncoding;
import static org.mule.runtime.core.internal.exception.ErrorTypeRepositoryFactory.createDefaultErrorTypeRepository;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.CONNECTION_PARAM;
import static org.mule.tck.MuleTestUtils.OBJECT_ERROR_TYPE_REPO_REGISTRY_KEY;
import static org.mule.tck.probe.PollingProber.probe;
import static org.mule.tck.util.MuleContextUtils.eventBuilder;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.message.api.MessageMetadataTypeBuilder;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.model.ConnectableComponentModel;
import org.mule.runtime.api.meta.model.OutputModel;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.lifecycle.LifecycleUtils;
import org.mule.runtime.core.api.streaming.DefaultStreamingManager;
import org.mule.runtime.core.api.streaming.bytes.InMemoryCursorStreamConfig;
import org.mule.runtime.core.api.streaming.bytes.factory.InMemoryCursorStreamProviderFactory;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.core.internal.streaming.bytes.ManagedCursorStreamProvider;
import org.mule.runtime.core.internal.streaming.bytes.SimpleByteBufferManager;
import org.mule.runtime.core.internal.util.message.ResultsToMessageList;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.loader.java.property.MediaTypeModelProperty;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public abstract class ValueReturnDelegateContractTestCase extends AbstractMuleContextTestCase {

  public static final String HELLO_WORLD_MSG = "Hello world!";

  @Mock(lenient = true)
  protected ExecutionContextAdapter operationContext;

  @Mock
  protected ConnectableComponentModel componentModel;

  protected CoreEvent event;

  @Mock
  protected Object attributes;

  @Mock(lenient = true)
  protected OutputModel outputModel;

  @Mock
  protected ConnectionHandler connectionHandler;

  protected ReturnDelegate delegate;
  protected ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();
  protected DefaultStreamingManager streamingManager;

  @Before
  public void before() throws MuleException {
    streamingManager = new DefaultStreamingManager();
    LifecycleUtils.initialiseIfNeeded(streamingManager, muleContext);

    event = eventBuilder(muleContext).message(Message.builder().value("").attributesValue(attributes).build()).build();

    when(outputModel.getType()).thenReturn(BaseTypeBuilder.create(JAVA).voidType().build());
    when(outputModel.getModelProperty(any())).thenReturn(empty());
    when(outputModel.getModelProperties()).thenReturn(emptySet());
    when(outputModel.getDescription()).thenReturn("");

    when(componentModel.getModelProperty(MediaTypeModelProperty.class)).thenReturn(empty());
    when(componentModel.getOutput()).thenReturn(outputModel);

    delegate = createReturnDelegate();
    when(operationContext.getEvent()).thenReturn(event);
    when(operationContext.getMuleContext()).thenReturn(muleContext);
    when(operationContext.getComponentModel()).thenReturn(componentModel);
    when(operationContext.getVariable(contains(CONNECTION_PARAM))).thenReturn(connectionHandler);
  }

  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    return singletonMap(OBJECT_ERROR_TYPE_REPO_REGISTRY_KEY, createDefaultErrorTypeRepository());
  }

  @After
  public void tearDown() throws Exception {
    disposeStreamingManager();
  }

  @Test
  public void returnsSingleValue() {
    byte[] value = new byte[] {};
    CoreEvent result = delegate.asReturnValue(value, operationContext);

    Message message = getOutputMessage(result);

    assertThat(message.getPayload().getValue(), is(sameInstance(value)));
    assertThat(message.getPayload().getDataType().getType().equals(byte[].class), is(true));
  }

  @Test
  public void operationReturnsOperationResultButKeepsAttributes() throws Exception {
    Object payload = new Object();
    MediaType mediaType = ANY.withCharset(getDefaultEncoding(muleContext));

    CoreEvent result =
        delegate.asReturnValue(Result.builder().output(payload).mediaType(mediaType).build(), operationContext);

    Message message = getOutputMessage(result);

    assertThat(message.getPayload().getValue(), is(sameInstance(payload)));
    assertThat(message.getAttributes().getValue(), is(nullValue()));
    assertThat(message.getPayload().getDataType().getMediaType(), equalTo(mediaType));
  }

  @Test
  public void operationReturnsOperationResultThatOnlySpecifiesPayload() throws Exception {
    Object payload = "hello world!";

    CoreEvent result = delegate.asReturnValue(Result.builder().output(payload).build(), operationContext);

    Message message = getOutputMessage(result);

    assertThat(message.getPayload().getValue(), is(sameInstance(payload)));
    assertThat(message.getAttributes().getValue(), is(nullValue()));
    assertThat(message.getPayload().getDataType().getType().equals(String.class), is(true));
  }

  @Test
  public void operationReturnsOperationResultThatOnlySpecifiesPayloadAndAttributes() throws Exception {
    Object payload = "hello world!";
    Object newAttributes = mock(Object.class);

    CoreEvent result =
        delegate.asReturnValue(Result.builder().output(payload).attributes(newAttributes).build(), operationContext);

    Message message = getOutputMessage(result);

    assertThat(message.getPayload().getValue(), is(sameInstance(payload)));
    assertThat(message.getAttributes().getValue(), is(sameInstance(newAttributes)));
    assertThat(message.getPayload().getDataType().getType().equals(String.class), is(true));
  }

  @Test
  public void operationWithDefaultMimeType() throws Exception {
    when(componentModel.getModelProperty(MediaTypeModelProperty.class)).thenReturn(Optional.of(
                                                                                               new MediaTypeModelProperty(APPLICATION_JSON
                                                                                                   .toRfcString(), true)));
    delegate = createReturnDelegate();

    Object value = "Hello world!";
    CoreEvent result = delegate.asReturnValue(value, operationContext);

    Message message = getOutputMessage(result);

    assertThat(message.getPayload().getValue(), is(sameInstance(value)));
    assertThat(message.getPayload().getDataType().getType().equals(String.class), is(true));
    assertThat(message.getPayload().getDataType().getMediaType().toRfcString(), containsString(APPLICATION_JSON.toRfcString()));
  }

  @Test
  public void operationWithPlainInputStreamOutput() throws Exception {
    when(outputModel.getType()).thenReturn(typeLoader.load(InputStream.class));
    when(componentModel.supportsStreaming()).thenReturn(true);
    assertStreamIsWrapped(new ByteArrayInputStream(HELLO_WORLD_MSG.getBytes(UTF_8)));
  }

  @Test
  public void operationWithResultInputStreamOutput() throws Exception {
    when(outputModel.getType()).thenReturn(typeLoader.load(InputStream.class));
    when(componentModel.supportsStreaming()).thenReturn(true);
    assertStreamIsWrapped(Result.builder().output(new ByteArrayInputStream(HELLO_WORLD_MSG.getBytes(UTF_8))).build());
  }

  @Test
  public void operationWithResultInputStreamCollectionOutput() throws Exception {
    MetadataType metadataType = BaseTypeBuilder.create(JAVA).arrayType().of(new MessageMetadataTypeBuilder().build()).build();
    when(outputModel.getType()).thenReturn(metadataType);
    when(componentModel.supportsStreaming()).thenReturn(true);

    delegate = createReturnDelegate();

    List<Result> resultList = new ArrayList<>();
    resultList.add(Result.builder().output(new ByteArrayInputStream(HELLO_WORLD_MSG.getBytes(UTF_8))).build());

    CoreEvent result = delegate.asReturnValue(resultList, operationContext);

    Message message = getOutputMessage(result);
    Message message1 = ((ResultsToMessageList) message.getPayload().getValue()).get(0);

    ManagedCursorStreamProvider actual = (ManagedCursorStreamProvider) message1.getPayload().getValue();
    InputStream resultingStream = actual.openCursor();
    assertThat(IOUtils.toString(resultingStream), is(HELLO_WORLD_MSG));
    resultingStream.close();
    actual.releaseResources();
    disposeStreamingManager();
    probe(5000, 500, () -> {
      verify(connectionHandler, atLeastOnce()).release();
      return true;
    });
  }

  @Test
  public void operationWithResultInputStreamOutputReleasesConnection() throws Exception {
    when(outputModel.getType()).thenReturn(typeLoader.load(InputStream.class));
    when(componentModel.supportsStreaming()).thenReturn(true);

    delegate = createReturnDelegate();

    Result<InputStream, Void> inputStreamResult =
        Result.<InputStream, Void>builder().output(new ByteArrayInputStream(HELLO_WORLD_MSG.getBytes(UTF_8))).build();

    CoreEvent result = delegate.asReturnValue(inputStreamResult, operationContext);

    Message message = getOutputMessage(result);

    ManagedCursorStreamProvider cursorStreamProvider = (ManagedCursorStreamProvider) message.getPayload().getValue();
    InputStream resultingStream = cursorStreamProvider.openCursor();
    assertThat(IOUtils.toString(resultingStream), is(HELLO_WORLD_MSG));
    verify(connectionHandler, atLeastOnce()).release();
  }

  private void assertStreamIsWrapped(Object value) throws InitialisationException, IOException {
    delegate = createReturnDelegate();
    CoreEvent result = delegate.asReturnValue(value, operationContext);

    Message message = getOutputMessage(result);

    ManagedCursorStreamProvider actual = (ManagedCursorStreamProvider) message.getPayload().getValue();
    InputStream resultingStream = actual.openCursor();
    assertThat(IOUtils.toString(resultingStream), is(HELLO_WORLD_MSG));
    resultingStream.close();
    actual.releaseResources();
    disposeStreamingManager();
    probe(5000, 500, () -> {
      verify(connectionHandler, atLeastOnce()).release();
      return true;
    });
  }

  private void disposeStreamingManager() {
    if (streamingManager != null) {
      streamingManager.dispose();
      streamingManager = null;
    }
  }

  protected InMemoryCursorStreamProviderFactory getCursorProviderFactory() {
    return new InMemoryCursorStreamProviderFactory(new SimpleByteBufferManager(),
                                                   InMemoryCursorStreamConfig.getDefault(),
                                                   streamingManager);
  }

  protected abstract ReturnDelegate createReturnDelegate() throws InitialisationException;

  protected abstract Message getOutputMessage(CoreEvent result);
}
