/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.processor;

import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.dsl.api.component.AbstractAnnotatedObjectFactory;
import org.mule.runtime.dsl.api.component.ObjectFactory;

/**
 * {@link ObjectFactory} to be used when there's just a configuration element that is a wrapper for a single message processor.
 *
 * i.e.: <dead-letter-queue><whatever-mp/></dead-letter-queue>
 *
 * @since 4.0
 */
public class MessageProcessorWrapperObjectFactory extends AbstractAnnotatedObjectFactory<Processor> {

  private Processor messageProcessor;

  public void setMessageProcessor(Processor messageProcessor) {
    this.messageProcessor = messageProcessor;
  }

  @Override
  public Processor doGetObject() throws Exception {
    return messageProcessor;
  }
}
