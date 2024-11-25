/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.processor.chain;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.newChain;

import static java.util.Optional.empty;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.mule.runtime.core.api.context.notification.MuleContextListener;
import org.mule.runtime.core.internal.context.DefaultMuleContext;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.testmodels.mule.TestMessageProcessor;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;



public class AbstractMessageProcessorChainTestCase extends AbstractMuleTestCase {

  @InjectMocks
  MessageProcessorChain messageChain = newChain(empty(), new TestMessageProcessor("test"));

  @Mock
  DefaultMuleContext muleContext;

  public AbstractMessageProcessorChainTestCase() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void testRemoveChain() throws Exception {
    startIfNeeded(messageChain);
    stopIfNeeded(messageChain);

    ArgumentCaptor<MuleContextListener> listenerCaptor = ArgumentCaptor.forClass(MuleContextListener.class);

    verify(muleContext, times(1)).addListener(listenerCaptor.capture());
    verify(muleContext, times(1)).removeListener(listenerCaptor.capture());

    assertThat(listenerCaptor.getValue(), is(sameInstance(listenerCaptor.getValue())));
  }
}
