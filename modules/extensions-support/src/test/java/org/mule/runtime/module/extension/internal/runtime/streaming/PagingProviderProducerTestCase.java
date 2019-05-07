/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.streaming;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.connectivity.ExtensionConnectionSupplier;
import org.mule.tck.size.SmallTest;

import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class PagingProviderProducerTestCase {

  private ExtensionConnectionSupplier extensionConnectionSupplier = mock(ExtensionConnectionSupplier.class);
  private ExecutionContextAdapter executionContext = mock(ExecutionContextAdapter.class);
  private PagingProvider<Object, String> delegate = mock(PagingProvider.class);
  private ConfigurationInstance config = mock(ConfigurationInstance.class);

  @InjectMocks
  private PagingProviderProducer<String> producer = createProducer();

  private PagingProviderProducer<String> createProducer() {
    return new PagingProviderProducer<>(delegate, config, executionContext, extensionConnectionSupplier);
  }

  @Before
  public void setUp() throws MuleException {
    ConnectionHandler handler = mock(ConnectionHandler.class);
    when(handler.getConnection()).thenReturn(new Object());
    when(extensionConnectionSupplier.getConnection(executionContext)).thenReturn(handler);

    ExtensionModel extensionModel = mock(ExtensionModel.class);
    when(executionContext.getExtensionModel()).thenReturn(extensionModel);
  }

  @Test
  public void produce() throws Exception {
    List<String> page = asList("bleh");
    when(delegate.getPage(anyObject())).thenReturn(page);
    assertThat(page, sameInstance(producer.produce()));
  }

  @Test
  public void produceWithDifferentConnections() throws Exception {
    ConnectionHandler connectionHandler = mock(ConnectionHandler.class);
    when(extensionConnectionSupplier.getConnection(any())).thenReturn(connectionHandler);

    produce();
    produce();

    verify(connectionHandler, times(2)).getConnection();
    verify(connectionHandler, times(2)).release();
  }

  @Test
  public void produceWithStickyConnection() throws Exception {
    when(delegate.useStickyConnections()).thenReturn(true);
    producer = createProducer();

    ConnectionHandler connectionHandler = mock(ConnectionHandler.class);
    when(extensionConnectionSupplier.getConnection(any())).thenReturn(connectionHandler);

    produce();
    produce();

    verify(connectionHandler, times(1)).getConnection();
    verify(connectionHandler, never()).release();

    producer.close();
    verify(connectionHandler).release();
  }

  @Test
  public void totalAvailable() {
    final int total = 10;
    when(delegate.getTotalResults(anyObject())).thenReturn(Optional.of(total));
    assertThat(total, is(producer.getSize()));
  }

  @Test
  public void closeQuietly() throws Exception {
    ConnectionHandler connectionHandler = mock(ConnectionHandler.class);
    when(extensionConnectionSupplier.getConnection(any())).thenReturn(connectionHandler);

    producer.close();
    verify(delegate).close(any());
    verify(connectionHandler, times(1)).release();
  }

  @Test(expected = Exception.class)
  public void closeNoisely() throws Exception {
    doThrow(new DefaultMuleException(new Exception())).when(delegate).close(any());
    producer.close();
  }
}
