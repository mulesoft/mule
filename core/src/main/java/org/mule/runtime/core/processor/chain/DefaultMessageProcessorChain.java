/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.chain;

import static java.util.Collections.singletonList;
import org.mule.runtime.core.api.processor.Processor;

import java.util.List;

public class DefaultMessageProcessorChain extends AbstractMessageProcessorChain {

  private Processor head;
  private List<Processor> processorsForLifecycle;

  DefaultMessageProcessorChain(String name, Processor head, List<Processor> processors,
                               List<Processor> processorsForLifecycle) {
    super(name, processors);
    this.head = head;
    this.processorsForLifecycle = processorsForLifecycle;
  }

  @Override
  protected List<Processor> getMessageProcessorsForLifecycle() {
    return processorsForLifecycle;
  }

  @Override
  protected List<Processor> getProcessorsToExecute() {
    return singletonList(head);
  }
}
