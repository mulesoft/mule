/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.streaming;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class PagingProviderProducerTestCase {

  private ConnectionManager connectionManager = mock(ConnectionManager.class);
  private PagingProvider<Object, String> delegate = mock(PagingProvider.class);
  private ConfigurationInstance config = mock(ConfigurationInstance.class);

  @InjectMocks
  private PagingProviderProducer<String> producer = new PagingProviderProducer<>(delegate, config, connectionManager);

  @Before
  public void setUp() throws ConnectionException {
    when(config.getValue()).thenReturn("config");
    ConnectionHandler handler = mock(ConnectionHandler.class);
    when(handler.getConnection()).thenReturn(new Object());
    when(connectionManager.getConnection(anyObject())).thenReturn(handler);
  }

  @Test
  public void produce() throws Exception {
    List<String> page = new ArrayList<>();
    when(delegate.getPage(anyObject())).thenReturn(page);
    assertThat(page, sameInstance(producer.produce()));
  }

  @Test
  public void totalAvailable() {
    final int total = 10;
    when(delegate.getTotalResults(anyObject())).thenReturn(Optional.of(total));
    assertThat(total, is(producer.size()));
  }

  @Test
  public void closeQuietly() throws Exception {
    producer.close();
    verify(delegate).close();
  }

  @Test(expected = Exception.class)
  public void closeNoisely() throws Exception {
    doThrow(new DefaultMuleException(new Exception())).when(delegate).close();
    producer.close();

  }
}
