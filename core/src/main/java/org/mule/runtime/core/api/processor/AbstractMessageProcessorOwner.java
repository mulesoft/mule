/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.processor;

import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.core.api.context.MuleContextAware;

import java.util.List;

/**
 * An object that owns message processors and delegates startup/shutdown events to them.
 */
public abstract class AbstractMessageProcessorOwner extends AbstractMuleObjectOwner<Processor>
    implements Lifecycle, MuleContextAware, Component {

  @Override
  protected List<Processor> getOwnedObjects() {
    return getOwnedMessageProcessors();
  }

  protected abstract List<Processor> getOwnedMessageProcessors();

}

