/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;

public class TestNotSerializableMessageProcessor extends AbstractComponent implements Processor {

  public TestNotSerializableMessageProcessor() {
    super();
  }

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    return event;
  }
}
