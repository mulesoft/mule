/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.privileged.processor.chain;

import org.mule.runtime.api.component.execution.ExecutableComponent;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.processor.InterceptingMessageProcessor;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.privileged.processor.Scope;

import java.util.List;

/**
 * A chain of {@link Processor}'s. All implementations should propagate {@link MuleContext}, {@link FlowConstruct} and lifecycle
 * to {@link Processor}'s in the chains.
 */
public interface MessageProcessorChain extends ExecutableComponent, Lifecycle, MuleContextAware, Scope {

  /**
   * Obtain the list of {@link Processor}'s that this chains was created from. Note that this is the linear view of all processors
   * that the chains was constructed from and does not represent in any way the structure of the chain once
   * {@link InterceptingMessageProcessor}'s have been taken into account.
   *
   * @return list of processors.
   */
  List<Processor> getMessageProcessors();

}
