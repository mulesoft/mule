/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.internal.client;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.MessageExchangePattern.ONE_WAY;
import static org.mule.runtime.core.MessageExchangePattern.REQUEST_RESPONSE;
import static org.mule.runtime.core.api.client.SimpleOptionsBuilder.newOptions;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.client.SimpleOptionsBuilder;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.functional.client.TestConnectorConfig;
import org.mule.functional.client.TestConnectorMessageProcessorProvider;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Before;
import org.junit.Test;

public class TestConnectorMessageProcessorProviderTestCase extends AbstractMuleTestCase {

  private static final String PATH_URL = "test://path";
  private static final String ANOTHER_PATH = "test://another";

  private final MuleContext muleContext = mock(MuleContext.class, RETURNS_DEEP_STUBS.get());
  private final TestConnectorMessageProcessorProvider messageProcessorProvider = new TestConnectorMessageProcessorProvider();
  private final TestConnectorConfig connectorConfig = new TestConnectorConfig();

  @Before
  public void setUp() throws Exception {
    when(muleContext.getRegistry().get(TestConnectorConfig.DEFAULT_CONFIG_ID)).thenReturn(connectorConfig);
    messageProcessorProvider.setMuleContext(muleContext);
  }

  @Test
  public void sameConfigReturnsSameInstanceUsingGenericOptions() throws Exception {
    final MessageProcessor messageProcessor =
        messageProcessorProvider.getMessageProcessor(PATH_URL, newOptions().build(), REQUEST_RESPONSE);

    assertThat(messageProcessor, is(not(nullValue())));
    assertThat(messageProcessorProvider.getMessageProcessor(PATH_URL, newOptions().build(), REQUEST_RESPONSE),
               is(messageProcessor));
  }

  @Test
  public void sameConfigReturnsDifferentInstanceUsingDifferentGenericOptions() throws Exception {
    final MessageProcessor messageProcessor1 =
        messageProcessorProvider.getMessageProcessor(PATH_URL, newOptions().build(), REQUEST_RESPONSE);
    final MessageProcessor messageProcessor2 =
        messageProcessorProvider.getMessageProcessor(PATH_URL, newOptions().responseTimeout(1000).build(), REQUEST_RESPONSE);

    assertThat(messageProcessor2, not(is(messageProcessor1)));
  }

  @Test
  public void differentPathReturnsDifferentOperations() throws Exception {
    final MessageProcessor messageProcessor1 =
        messageProcessorProvider.getMessageProcessor(PATH_URL, newOptions().build(), ONE_WAY);
    final MessageProcessor messageProcessor2 =
        messageProcessorProvider.getMessageProcessor(ANOTHER_PATH, newOptions().build(), ONE_WAY);

    assertThat(messageProcessor2, not(is(messageProcessor1)));
  }

  @Test
  public void differentExchangePatternsReturnsDifferentOperations() throws Exception {
    final MessageProcessor messageProcessor1 =
        messageProcessorProvider.getMessageProcessor(PATH_URL, newOptions().build(), ONE_WAY);
    final MessageProcessor messageProcessor2 =
        messageProcessorProvider.getMessageProcessor(PATH_URL, newOptions().build(), REQUEST_RESPONSE);

    assertThat(messageProcessor2, not(is(messageProcessor1)));
  }

  @Test
  public void disposeInvalidatesCache() throws Exception {
    final MessageProcessor messageProcessor1 =
        messageProcessorProvider.getMessageProcessor(PATH_URL, SimpleOptionsBuilder.newOptions().build(), REQUEST_RESPONSE);
    messageProcessorProvider.dispose();

    final MessageProcessor messageProcessor2 =
        messageProcessorProvider.getMessageProcessor(PATH_URL, SimpleOptionsBuilder.newOptions().build(), REQUEST_RESPONSE);

    assertThat(messageProcessor2, not(is(messageProcessor1)));
  }

  @Test(expected = IllegalArgumentException.class)
  public void doNotAllowNullUrl() throws Exception {
    messageProcessorProvider.getMessageProcessor(null, newOptions().build(), ONE_WAY);
  }

  @Test(expected = IllegalArgumentException.class)
  public void doNotAllowNullOptions() throws Exception {
    messageProcessorProvider.getMessageProcessor(PATH_URL, null, ONE_WAY);
  }

  @Test(expected = IllegalArgumentException.class)
  public void doNotAllowNullExchangePattern() throws Exception {
    messageProcessorProvider.getMessageProcessor(PATH_URL, newOptions().build(), null);
  }
}
