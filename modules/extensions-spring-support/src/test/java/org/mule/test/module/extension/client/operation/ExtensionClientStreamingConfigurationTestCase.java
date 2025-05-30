/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.client.operation;

import static org.mule.runtime.api.util.DataUnit.BYTE;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STREAMING_MANAGER;
import static org.mule.test.allure.AllureConstants.ExtensionsClientFeature.EXTENSIONS_CLIENT;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.HEISENBERG;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.api.util.DataSize;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.api.streaming.bytes.FileStoreCursorStreamConfig;
import org.mule.runtime.core.api.streaming.bytes.InMemoryCursorStreamConfig;
import org.mule.runtime.core.api.streaming.object.FileStoreCursorIteratorConfig;
import org.mule.runtime.core.api.streaming.object.InMemoryCursorIteratorConfig;
import org.mule.runtime.extension.api.client.ExtensionsClient;
import org.mule.test.module.extension.AbstractHeisenbergConfigTestCase;

import java.util.List;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import io.qameta.allure.Feature;
import jakarta.inject.Inject;

@Feature(EXTENSIONS_CLIENT)
public class ExtensionClientStreamingConfigurationTestCase extends AbstractHeisenbergConfigTestCase {

  private static final String HEISENBERG_EXT_NAME = HEISENBERG;
  private static final String HEISENBERG_CONFIG = "heisenberg";
  private static final String ITERABLE_OPERATION = "getPagedBlocklist";
  private static final String STREAMING_OPERATION = "nameAsStream";

  @Inject
  private ExtensionsClient client;

  private StreamingManager streamingManager;

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"operations/heisenberg-config.xml"};
  }

  @Override
  protected void addBuilders(List<ConfigurationBuilder> builders) {
    streamingManager = mock(StreamingManager.class, RETURNS_DEEP_STUBS);
    builders.add(new AbstractConfigurationBuilder() {

      @Override
      public void doConfigure(MuleContext muleContext) {
        muleContext.getCustomizationService().overrideDefaultServiceImpl(OBJECT_STREAMING_MANAGER, streamingManager);
      }
    });

    super.addBuilders(builders);
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    setDisposeContextPerClass(true);
    when(streamingManager.manage(any(CursorProvider.class), any(EventContext.class))).thenAnswer(inv -> inv.getArguments()[0]);
  }

  @Test
  public void customInMemoryRepeatableIterable() throws Exception {
    final int initialBufferSize = 3;
    final int initialBufferSizeIncrement = 5;
    final int maxBufferSize = 11;

    ArgumentCaptor<InMemoryCursorIteratorConfig> captor = forClass(InMemoryCursorIteratorConfig.class);

    client.execute(HEISENBERG_EXT_NAME, ITERABLE_OPERATION, params -> params.withConfigRef(HEISENBERG_CONFIG)
        .withInMemoryRepeatableIterables(initialBufferSize, initialBufferSizeIncrement, maxBufferSize))
        .get();

    verify(streamingManager.forObjects()).getInMemoryCursorProviderFactory(captor.capture());
    InMemoryCursorIteratorConfig config = captor.getValue();
    assertThat(config.getInitialBufferSize(), is(initialBufferSize));
    assertThat(config.getBufferSizeIncrement(), is(initialBufferSizeIncrement));
    assertThat(config.getMaxBufferSize(), is(maxBufferSize));
  }

  @Test
  public void customFileStoreRepeatableIterable() throws Exception {
    final int maxBufferSize = 11;

    ArgumentCaptor<FileStoreCursorIteratorConfig> captor = forClass(FileStoreCursorIteratorConfig.class);

    client
        .execute(HEISENBERG_EXT_NAME, ITERABLE_OPERATION,
                 params -> params.withConfigRef(HEISENBERG_CONFIG).withFileStoreRepeatableIterables(maxBufferSize))
        .get();

    verify(streamingManager.forObjects()).getFileStoreCursorIteratorProviderFactory(captor.capture());
    FileStoreCursorIteratorConfig config = captor.getValue();
    assertThat(config.getMaxInMemoryInstances(), is(maxBufferSize));
  }

  @Test
  public void customInMemoryRepeatableStream() throws Throwable {
    final DataSize initialBufferSize = new DataSize(3, BYTE);
    final DataSize initialBufferSizeIncrement = new DataSize(5, BYTE);
    final DataSize maxBufferSize = new DataSize(11, BYTE);

    ArgumentCaptor<InMemoryCursorStreamConfig> captor = forClass(InMemoryCursorStreamConfig.class);

    client.execute(HEISENBERG_EXT_NAME, STREAMING_OPERATION, params -> params.withConfigRef(HEISENBERG_CONFIG)
        .withInMemoryRepeatableStreaming(initialBufferSize, initialBufferSizeIncrement, maxBufferSize))
        .get();

    verify(streamingManager.forBytes()).getInMemoryCursorProviderFactory(captor.capture());
    InMemoryCursorStreamConfig config = captor.getValue();

    assertThat(config.getInitialBufferSize(), equalTo(initialBufferSize));
    assertThat(config.getBufferSizeIncrement(), equalTo(initialBufferSizeIncrement));
    assertThat(config.getMaxBufferSize(), equalTo(maxBufferSize));
  }

  @Test
  public void customFileStoreRepeatableStream() throws Throwable {
    final DataSize maxInMemorySize = new DataSize(3, BYTE);

    ArgumentCaptor<FileStoreCursorStreamConfig> captor = forClass(FileStoreCursorStreamConfig.class);

    client.execute(HEISENBERG_EXT_NAME, STREAMING_OPERATION, params -> params.withConfigRef(HEISENBERG_CONFIG)
        .withFileStoreRepeatableStreaming(maxInMemorySize))
        .get();

    verify(streamingManager.forBytes()).getFileStoreCursorStreamProviderFactory(captor.capture());
    FileStoreCursorStreamConfig config = captor.getValue();

    assertThat(config.getMaxInMemorySize(), equalTo(maxInMemorySize));
  }
}
