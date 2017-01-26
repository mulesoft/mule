/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transaction;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.processor.DelegateTransactionFactory;
import org.mule.runtime.core.processor.TransactionalInterceptingMessageProcessor;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class TransactionalBlockTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "transactional-block-config.xml";
  }

  @Test
  public void resolvesStandardTransactionFactory() throws Exception {
    MessageProcessorChain blockChain =
        (MessageProcessorChain) ((Flow) getFlowConstruct("standardBlock")).getMessageProcessors().get(0);
    Processor firstProcessor = ((MessageProcessorChain) blockChain.getMessageProcessors().get(0)).getMessageProcessors().get(0);
    assertThat(firstProcessor,
               is(instanceOf(TransactionalInterceptingMessageProcessor.class)));

    TransactionalInterceptingMessageProcessor block = (TransactionalInterceptingMessageProcessor) firstProcessor;
    assertThat(block.getTransactionConfig().getFactory(), is(instanceOf(DelegateTransactionFactory.class)));
  }

}
