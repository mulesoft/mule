/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.chain;

import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.processor.Processor;

import java.util.List;

public class SimpleMessageProcessorChain extends DefaultMessageProcessorChain {

  public SimpleMessageProcessorChain(List<Processor> processors) {
    super(processors);
  }

  public SimpleMessageProcessorChain(Processor... processors) {
    super(processors);
  }

  public SimpleMessageProcessorChain(String name, List<Processor> processors) {
    super(name, processors);
  }

  public SimpleMessageProcessorChain(String name, Processor... processors) {
    super(name, processors);
  }

  @Override
  protected Event doProcess(Event event) throws MuleException {
    for (int i = 0; i < processors.size(); i++) {
      Processor processor = processors.get(i);
      event = messageProcessorExecutionTemplate.execute(processor, event);
      if (event == null) {
        return null;
      } else if (event instanceof VoidMuleEvent) {
        return event;
      }
    }
    return event;
  }

}
