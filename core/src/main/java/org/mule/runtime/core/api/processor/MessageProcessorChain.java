/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.processor;

import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.meta.AnnotatedObject;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.MuleContextAware;

import java.util.List;

/**
 * A chain of {@link Processor}'s. All implementioans should propagate {@link MuleContext}, {@link FlowConstruct} and lifecycle to
 * {@link Processor}'s in the chains. Message processor chains are also {@link MessageProcessorContainer}'s and responsible for
 * adding the correct {@link MessageProcessorPathElement}'s to their parent {@link MessageProcessorPathElement}.
 */
public interface MessageProcessorChain
    extends Processor, Lifecycle, MuleContextAware, AnnotatedObject, Scope {

  /**
   * Obtain the list of {@link Processor}'s that this chains was created from. Note that this is the linear view of all processors
   * that the chains was constructed from and does not represent in any way the structure of the chain once
   * {@link InterceptingMessageProcessor}'s have been taken into account.
   *
   * @return list of processors.
   */
  List<Processor> getMessageProcessors();

}
