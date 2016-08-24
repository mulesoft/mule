/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.chain;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.connector.ReplyToHandler;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.ProcessorExecutor;
import org.mule.runtime.core.processor.BlockingProcessorExecutor;
import org.mule.runtime.core.processor.NonBlockingProcessorExecutor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class ProcessorExecutorFactoryTestCase extends AbstractMuleTestCase {

  @Mock
  private MuleEvent muleEvent;

  @Test
  public void flow() {
    when(muleEvent.isAllowNonBlocking()).thenReturn(false);
    assertThat(createProcessorExecutor(), instanceOf(BlockingProcessorExecutor.class));
  }

  @Test
  public void flowReplyHandler() {
    when(muleEvent.isAllowNonBlocking()).thenReturn(false);
    when(muleEvent.getReplyToHandler()).thenReturn(mock(ReplyToHandler.class));
    assertThat(createProcessorExecutor(), instanceOf(BlockingProcessorExecutor.class));
  }

  @Test
  public void flowNonBlockingAllowed() {
    when(muleEvent.isAllowNonBlocking()).thenReturn(true);
    assertThat(createProcessorExecutor(), instanceOf(NonBlockingProcessorExecutor.class));
  }

  @Test
  public void flowNonBlockingAllowedReplyHandler() {
    when(muleEvent.isAllowNonBlocking()).thenReturn(true);
    when(muleEvent.getReplyToHandler()).thenReturn(mock(ReplyToHandler.class));
    assertThat(createProcessorExecutor(), instanceOf(NonBlockingProcessorExecutor.class));
  }

  private ProcessorExecutor createProcessorExecutor() {
    return new ProcessorExecutorFactory().createProcessorExecutor(muleEvent, null, null, false, mock(FlowConstruct.class));
  }

}
