/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.factories;

import org.mule.runtime.core.privileged.processor.objectfactory.MessageProcessorChainObjectFactory;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChainBuilder;
import org.mule.runtime.core.internal.processor.chain.SubflowMessageProcessorChainBuilder;

/**
 * Uses a custom message processor chain builder for subflows in order to generate the proper message processor ids.
 */
public class SubflowMessageProcessorChainFactoryBean extends MessageProcessorChainObjectFactory {

  @Override
  protected MessageProcessorChainBuilder getBuilderInstance() {
    SubflowMessageProcessorChainBuilder builder = new SubflowMessageProcessorChainBuilder();
    builder.setName(name);
    return builder;
  }
}
