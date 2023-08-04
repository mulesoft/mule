/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.factories;

import org.mule.runtime.core.internal.routing.ProcessorRoute;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;

public class ProcessorRouteFactoryBean extends AbstractProcessorRouteFactoryBean<ProcessorRoute> {

  @Override
  protected ProcessorRoute getProcessorRoute(MessageProcessorChain chain) {
    return new ProcessorRoute(chain, componentTracerFactory);
  }
}
