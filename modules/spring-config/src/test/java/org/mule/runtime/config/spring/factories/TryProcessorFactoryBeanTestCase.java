/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.factories;

import static java.util.Collections.singletonMap;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.meta.AbstractAnnotatedObject.ROOT_CONTAINER_NAME_KEY;
import static org.mule.runtime.api.tx.TransactionType.LOCAL;
import static org.mule.runtime.core.api.transaction.MuleTransactionConfig.ACTION_INDIFFERENT_STRING;
import org.mule.runtime.config.spring.internal.factories.TryProcessorFactoryBean;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.internal.processor.TryScope;
import org.mule.runtime.core.internal.registry.SimpleRegistry;
import org.mule.runtime.core.internal.transaction.TransactionFactoryLocator;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TryProcessorFactoryBeanTestCase extends AbstractMuleTestCase {

  @Mock(answer = RETURNS_DEEP_STUBS)
  private MuleContext muleContextMock;

  private SimpleRegistry registry;

  @Before
  public void setUp() throws RegistrationException {
    when(muleContextMock.getRegistry().lookupObject(StreamingManager.class))
        .thenReturn(mock(StreamingManager.class));
    registry = new SimpleRegistry(muleContextMock);
    registry.registerObject("txFactory", new TransactionFactoryLocator());
  }

  @Test
  public void doesNotFailWithNoProcessors() throws Exception {
    TryProcessorFactoryBean tryProcessorFactoryBean = new TryProcessorFactoryBean();
    tryProcessorFactoryBean.setTransactionalAction(ACTION_INDIFFERENT_STRING);
    tryProcessorFactoryBean.setTransactionType(LOCAL);
    tryProcessorFactoryBean.setAnnotations(singletonMap(ROOT_CONTAINER_NAME_KEY, "root"));
    registry.inject(tryProcessorFactoryBean);

    TryScope tryMessageProcessor = (TryScope) tryProcessorFactoryBean.getObject();
    tryMessageProcessor.setMuleContext(muleContextMock);
    tryMessageProcessor.initialise();
    tryMessageProcessor.start();
  }

}
