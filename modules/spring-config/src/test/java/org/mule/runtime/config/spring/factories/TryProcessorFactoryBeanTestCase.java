/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.factories;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.transaction.MuleTransactionConfig.ACTION_INDIFFERENT_STRING;
import static org.mule.runtime.core.api.transaction.TransactionType.LOCAL;
import org.mule.runtime.config.spring.internal.factories.TryProcessorFactoryBean;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.internal.processor.TryScope;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

public class TryProcessorFactoryBeanTestCase extends AbstractMuleTestCase {

  @Test
  public void doesNotFailWithNoProcessors() throws Exception {
    TryProcessorFactoryBean tryProcessorFactoryBean = new TryProcessorFactoryBean();
    tryProcessorFactoryBean.setTransactionalAction(ACTION_INDIFFERENT_STRING);
    tryProcessorFactoryBean.setTransactionType(LOCAL);
    TryScope tryMessageProcessor = (TryScope) tryProcessorFactoryBean.getObject();
    MuleContext muleContextMock = mock(MuleContext.class, RETURNS_DEEP_STUBS);
    when(muleContextMock.getRegistry().lookupObject(StreamingManager.class))
        .thenReturn(mock(StreamingManager.class));
    tryMessageProcessor.setMuleContext(muleContextMock);
    tryMessageProcessor.initialise();
    tryMessageProcessor.start();
  }

}
