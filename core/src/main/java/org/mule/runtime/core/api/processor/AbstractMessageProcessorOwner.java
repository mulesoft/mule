/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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

